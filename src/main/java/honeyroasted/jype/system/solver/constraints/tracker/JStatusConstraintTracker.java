package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.almonds.Constraint;

import java.util.Collections;
import java.util.Stack;
import java.util.function.Consumer;

public class JStatusConstraintTracker implements JConstraintTracker {
    private Stack<JConstraintResult.Status> stack = new Stack<>();
    private Stack<JConstraintResult.Operator> opStac = new Stack<>();
    private JConstraintResult.Status head;
    private JConstraintResult.Operator headOp = JConstraintResult.Operator.SET;

    public void or() {
        stack.push(head);
        opStac.push(headOp);

        head = JConstraintResult.Status.UNKNOWN;
        headOp = JConstraintResult.Operator.OR;
    }

    public void and() {
        stack.push(head);
        opStac.push(headOp);

        head = JConstraintResult.Status.ASSUMED;
        headOp = JConstraintResult.Operator.AND;
    }

    @Override
    public JConstraintTracker with(Constraint constraint) {
        return this;
    }

    @Override
    public JConstraintTracker with(JConstraintResult.Status value) {
        switch (this.headOp) {
            case SET -> this.head = value;
            case AND -> this.head = this.head.and(value);
            case OR -> this.head = this.head.or(value);
        }
        return this;
    }

    @Override
    public JConstraintTracker parent() {
        JConstraintResult.Status curr = this.head;

        this.head = stack.pop();
        this.headOp = opStac.pop();

        return this.with(curr);
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
    public JConstraintTracker then(Consumer<JConstraintTracker> cons) {
        if (canChange()) {
            cons.accept(this);
        }
        return this;
    }

    @Override
    public boolean canChange() {
        return headOp == JConstraintResult.Operator.SET || (headOp == JConstraintResult.Operator.OR && head == JConstraintResult.Status.FALSE) || (headOp == JConstraintResult.Operator.AND && head == JConstraintResult.Status.TRUE);
    }

    @Override
    public JConstraintResult.Status status() {
        return head;
    }

    @Override
    public JConstraintResult result() {
        return new JConstraintResult(this.head, this.head.isTrue() ? Constraint.TRUE : Constraint.FALSE, JConstraintResult.Operator.SET, Collections.emptyList());
    }
}
