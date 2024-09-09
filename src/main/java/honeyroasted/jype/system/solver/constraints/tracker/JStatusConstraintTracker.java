package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.almonds.Constraint;

import java.util.Collections;
import java.util.Stack;
import java.util.function.Consumer;

public class JStatusConstraintTracker implements JConstraintTracker {
    private Stack<Boolean> stack = new Stack<>();
    private Stack<JConstraintResult.Operator> opStac = new Stack<>();
    private boolean head;
    private JConstraintResult.Operator headOp = JConstraintResult.Operator.SET;

    public void or() {
        stack.push(head);
        opStac.push(headOp);

        head = false;
        headOp = JConstraintResult.Operator.OR;
    }

    public void and() {
        stack.push(head);
        opStac.push(headOp);

        head = true;
        headOp = JConstraintResult.Operator.AND;
    }

    @Override
    public JConstraintTracker with(Constraint constraint) {
        return this;
    }

    @Override
    public JConstraintTracker with(boolean value) {
        switch (this.headOp) {
            case SET -> this.head = value;
            case AND -> this.head &= value;
            case OR -> this.head |= value;
        }
        return this;
    }

    @Override
    public JConstraintTracker parent() {
        boolean curr = this.head;

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
        return headOp == JConstraintResult.Operator.SET || (headOp == JConstraintResult.Operator.OR && head == false) || (headOp == JConstraintResult.Operator.AND && head == true);
    }

    @Override
    public boolean status() {
        return head;
    }

    @Override
    public JConstraintResult result() {
        return new JConstraintResult(this.head, this.head ? Constraint.TRUE : Constraint.FALSE, JConstraintResult.Operator.SET, Collections.emptyList());
    }
}
