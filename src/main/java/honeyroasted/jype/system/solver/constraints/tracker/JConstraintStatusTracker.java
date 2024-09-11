package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.jype.system.solver.constraints.JTypeConstraint;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;

public class JConstraintStatusTracker implements JConstraintTracker {
    private Stack<JConstraintResult.Status> stack = new Stack<>();
    private Stack<JConstraintResult.Operator> opStac = new Stack<>();
    private JConstraintResult.Status head;
    private JConstraintResult.Operator headOp = JConstraintResult.Operator.SET;

    @Override
    public JConstraintTracker with(JTypeConstraint constraint, JConstraintResult.Status value) {
        return set(value);
    }

    @Override
    public JConstraintTracker set(JConstraintResult.Status value) {
        switch (this.headOp) {
            case SET -> this.head = value;
            case AND -> this.head = this.head.and(value);
            case OR -> this.head = this.head.or(value);
        }
        return this;
    }

    @Override
    public JConstraintTracker push(JTypeConstraint constraint, JConstraintResult.Operator op) {
        stack.push(head);
        opStac.push(headOp);

        head = op.identity();
        headOp = op;
        return this;
    }

    @Override
    public JConstraintTracker pop(JConstraintResult.Operator op) {
        JConstraintResult.Status curr = this.head;

        this.head = stack.pop();
        this.headOp = opStac.pop();

        return this.set(curr);
    }

    @Override
    public JTypeConstraint peek() {
        return this.head.isTruthy() ? JTypeConstraint.TRUE : JTypeConstraint.FALSE;
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
        return new JConstraintResult(this.head, this.head.isTruthy() ? JTypeConstraint.TRUE : JTypeConstraint.FALSE, JConstraintResult.Operator.SET, Collections.emptyList());
    }

    @Override
    public Iterator<Map.Entry<JTypeConstraint, JConstraintResult.Status>> branchIterator() {
        return Map.of(this.head.isTruthy() ? JTypeConstraint.TRUE : JTypeConstraint.FALSE, this.head).entrySet().iterator();
    }
}
