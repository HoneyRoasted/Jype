package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.almonds.Constraint;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class JTreeConstraintTracker implements JConstraintTracker {
    private Stack<Entry> entryStack = new Stack<>();
    private Entry head;

    private boolean processIrrelevantBranches;

    public JTreeConstraintTracker(boolean processIrrelevantBranches) {
        this.processIrrelevantBranches = processIrrelevantBranches;
        this.head = new Entry();
    }

    public JTreeConstraintTracker() {
        this(false);
    }

    private void or() {
        Entry curr = this.head;
        entryStack.push(curr);

        head = new Entry();
        head.op = JConstraintResult.Operator.OR;
        head.success = false;

        curr.children.add(head);
    }

    private void and() {
        Entry curr = this.head;
        entryStack.push(curr);

        head = new Entry();
        head.op = JConstraintResult.Operator.AND;
        head.success = true;

        curr.children.add(head);
    }

    @Override
    public JConstraintTracker or(Consumer<JConstraintTracker>... actions) {
        this.or();
        for (Consumer<JConstraintTracker> action : actions) {
            this.then(action);
        }
        return parent();
    }

    @Override
    public JConstraintTracker and(Consumer<JConstraintTracker>... actions) {
        this.and();
        for (Consumer<JConstraintTracker> action : actions) {
            this.then(action);
        }
        return parent();
    }

    @Override
    public JConstraintTracker with(Constraint constraint) {
        head.constraint.add(constraint);
        return this;
    }

    @Override
    public JConstraintTracker with(boolean value) {
        switch (head.op) {
            case SET -> head.success = value;
            case AND -> head.success &= value;
            case OR -> head.success |= value;
        }
        return this;
    }

    @Override
    public JConstraintTracker parent() {
        Entry curr = head;
        head = entryStack.pop();

        return with(curr.success);
    }

    @Override
    public JConstraintTracker then(Consumer<JConstraintTracker> cons) {
        if (this.processIrrelevantBranches || canChange()) {
            cons.accept(this);
        }
        return this;
    }

    @Override
    public boolean canChange() {
        return head.op == JConstraintResult.Operator.SET || (head.op == JConstraintResult.Operator.OR && head.success == false) || (head.op == JConstraintResult.Operator.AND && head.success == true);
    }

    @Override
    public boolean status() {
        return head.success;
    }

    @Override
    public JConstraintResult result() {
        JConstraintResult result = this.head.toResult();
        return result != null ? result :
                new JConstraintResult(this.head.success, this.head.success ? Constraint.TRUE : Constraint.FALSE, JConstraintResult.Operator.SET, Collections.emptyList());
    }

    private static class Entry {
        public boolean success = false;
        public JConstraintResult.Operator op = JConstraintResult.Operator.SET;
        public Set<Constraint> constraint = new LinkedHashSet<>();
        public List<Entry> children = new ArrayList<>();

        private JConstraintResult toResult() {
            if (constraint.isEmpty() && children.isEmpty()) return null;
            if (constraint.isEmpty() && children.size() == 1) return children.getFirst().toResult();

            return new JConstraintResult(success,
                    constraint.isEmpty() ? (op == JConstraintResult.Operator.AND ? Constraint.and() : op == JConstraintResult.Operator.OR ? Constraint.or() : Constraint.solve()) :
                    constraint.size() == 1 ? constraint.iterator().next() : new JTypeConstraints.Multi(constraint),
                    children.size() == 1 ? children.getFirst().op : op,
                    children.stream()
                            .flatMap(entry -> entry.constraint.isEmpty() && !entry.children.isEmpty() &&
                                    (entry.op == op || children.size() == 1) ?
                                    entry.children.stream() : Stream.of(entry))
                            .map(Entry::toResult).filter(Objects::nonNull)
                            .toList());
        }
    }
}
