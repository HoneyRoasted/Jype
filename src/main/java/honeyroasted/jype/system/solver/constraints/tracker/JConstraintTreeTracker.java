package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.jype.system.solver.constraints.JTypeConstraint;

import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;

public class JConstraintTreeTracker implements JConstraintTracker {
    private Stack<JConstraintResult.Operator> operatorStack;
    private Stack<JTypeConstraint> constraintStack;
    private JConstraintTree head;

    private boolean processIrrelevantBranches;

    public JConstraintTreeTracker(boolean processIrrelevantBranches) {
        this.processIrrelevantBranches = processIrrelevantBranches;
        this.constraintStack = new Stack<>();
        this.operatorStack = new Stack<>();
        this.head = new JConstraintTree();

        this.constraintStack.push(JTypeConstraint.solve());
        this.operatorStack.push(JConstraintResult.Operator.AND);
    }

    public JConstraintTreeTracker() {
        this(false);
    }

    @Override
    public JConstraintTracker push(JTypeConstraint constraint, JConstraintResult.Operator op) {
        JConstraintResult.Operator prev = this.operatorStack.peek();
        this.constraintStack.push(constraint);
        this.operatorStack.push(op);
        if (op != prev && op == JConstraintResult.Operator.AND) {
            //OR -> AND
            this.head = this.head.addAndGetChild(new JConstraintTree(this.head));
        }
        return this;
    }

    @Override
    public JConstraintTracker pop(JConstraintResult.Operator op) {
        this.constraintStack.pop();
        this.operatorStack.pop();

        if (this.operatorStack.peek() != op && op == JConstraintResult.Operator.AND) {
            //AND -> OR
            this.head.integrateQueue();
            this.head = this.head.parent();
        }

        return this;
    }

    @Override
    public JTypeConstraint peek() {
        return this.constraintStack.peek();
    }

    @Override
    public JConstraintTracker with(JTypeConstraint constraint, JConstraintResult.Status value) {
        if (this.operatorStack.peek() == JConstraintResult.Operator.OR && this.operatorStack.get(operatorStack.size() - 2) == JConstraintResult.Operator.AND) {
            JConstraintTree newChild = new JConstraintTree(this.head);
            newChild.put(constraint, value);
            this.head.addAndGetChild(newChild);
        } else {
            this.head.queue(constraint, value);
        }
        return this;
    }

    @Override
    public JConstraintTracker set(JConstraintResult.Status value) {
        if (!this.constraintStack.isEmpty()) {
            this.with(this.constraintStack.peek(), value);
        }
        return this;
    }

    @Override
    public JConstraintTracker then(Consumer<JConstraintTracker> cons) {
        cons.accept(this);
        this.head.integrateQueue();
        return this;
    }

    @Override
    public boolean canChange() {
        return true;
    }

    @Override
    public JConstraintResult.Status status() {
        return this.head.status();
    }

    @Override
    public JConstraintResult result() {
        return this.head.toResult();
    }

    @Override
    public Iterator<Map.Entry<JTypeConstraint, JConstraintResult.Status>> branchIterator() {
        return this.head.andIterator();
    }
}
