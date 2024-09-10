package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.jype.system.solver.constraints.JTypeConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;

public class JConstraintResultTracker implements JConstraintTracker {
    private Stack<Entry> entryStack = new Stack<>();
    private Entry head;

    private boolean processIrrelevantBranches;

    public JConstraintResultTracker(boolean processIrrelevantBranches) {
        this.processIrrelevantBranches = processIrrelevantBranches;
        this.head = new Entry();
    }

    public JConstraintResultTracker() {
        this(false);
    }

    @Override
    public JConstraintTracker push(JTypeConstraint constraint, JConstraintResult.Operator op) {
        Entry curr = this.head;
        entryStack.push(curr);

        head = new Entry();
        head.parent = curr;
        head.constraint = constraint;
        head.success = op.identity();
        head.op = op;

        curr.children.add(head);
        return this;
    }

    @Override
    public JConstraintTracker pop(JConstraintResult.Operator op) {
        Entry curr = head;
        head = entryStack.pop();
        set(curr.success);
        return this;
    }

    @Override
    public JTypeConstraint peek() {
        return head.constraint;
    }

    @Override
    public JConstraintTracker with(JTypeConstraint constraint, JConstraintResult.Status value) {
        if (head.constraint == null) {
            head.constraint = constraint;
        } else {
            Entry child = new Entry();
            child.constraint = constraint;
            child.success = value;
            head.children.add(child);
        }
        set(value);
        return this;
    }

    @Override
    public JConstraintTracker set(JConstraintResult.Status value) {
        switch (head.op) {
            case SET -> head.success = value;
            case AND -> head.success = head.success.and(value);
            case OR -> head.success = head.success.or(value);
        }
        return this;
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
        return head.op == JConstraintResult.Operator.SET || (head.op == JConstraintResult.Operator.OR && head.success != JConstraintResult.Status.TRUE) || (head.op == JConstraintResult.Operator.AND && head.success != JConstraintResult.Status.FALSE);
    }

    @Override
    public JConstraintResult.Status status() {
        return head.success;
    }

    @Override
    public JConstraintResult result() {
        JConstraintResult result = this.head.toResult();
        return result != null ? result :
                new JConstraintResult(this.head.success, this.head.success.isTrue() ? JTypeConstraint.TRUE : JTypeConstraint.FALSE, JConstraintResult.Operator.SET, Collections.emptyList());
    }

    @Override
    public Iterator<Map.Entry<JTypeConstraint, JConstraintResult.Status>> branchIterator() {
        throw new UnsupportedOperationException();
    }

    private static class Entry implements Map.Entry<JTypeConstraint, JConstraintResult.Status> {
        public Entry parent;

        public JConstraintResult.Status success = JConstraintResult.Status.UNKNOWN;
        public JConstraintResult.Operator op = JConstraintResult.Operator.SET;
        public JTypeConstraint constraint;
        public List<Entry> children = new ArrayList<>();

        private JConstraintResult toResult() {
            return new JConstraintResult(success, constraint == null ?
                    switch (op) {case SET -> JTypeConstraint.solve(); case AND -> JTypeConstraint.and(); case OR -> JTypeConstraint.or();}
                    : constraint, op, children.stream().map(Entry::toResult).toList());

        }

        @Override
        public JTypeConstraint getKey() {
            return constraint;
        }

        @Override
        public JConstraintResult.Status getValue() {
            return success;
        }

        @Override
        public JConstraintResult.Status setValue(JConstraintResult.Status value) {
            JConstraintResult.Status prev = this.success;
            this.success = value;
            return prev;
        }
    }
}
