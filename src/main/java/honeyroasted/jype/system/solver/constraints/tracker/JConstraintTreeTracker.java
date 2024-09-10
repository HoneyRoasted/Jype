package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.jype.system.solver.constraints.JTypeConstraint;

import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;

public class JConstraintTreeTracker implements JConstraintTracker {
    private Stack<JConstraintTree> treeStack;
    private JConstraintTree head;

    private boolean processIrrelevantBranches;

    public JConstraintTreeTracker(boolean processIrrelevantBranches) {
        this.processIrrelevantBranches = processIrrelevantBranches;
        this.treeStack = new Stack<>();
    }

    public JConstraintTreeTracker() {
        this(false);
    }

    @Override
    public JConstraintTracker push(JTypeConstraint constraint, JConstraintResult.Operator op) {
        return this;
    }

    @Override
    public JConstraintTracker pop(JConstraintResult.Operator op) {
        return this;
    }

    @Override
    public JTypeConstraint peek() {
        return null;
    }

    @Override
    public JConstraintTracker with(JTypeConstraint constraint, JConstraintResult.Status value) {
        this.head.put(constraint, value);
        return this;
    }

    @Override
    public JConstraintTracker set(JConstraintResult.Status value) {
        return this;
    }

    @Override
    public JConstraintTracker then(Consumer<JConstraintTracker> cons) {
        return this;
    }

    @Override
    public boolean canChange() {
        return false;
    }

    @Override
    public JConstraintResult.Status status() {
        return null;
    }

    @Override
    public JConstraintResult result() {
        return null;
    }

    @Override
    public Iterator<Map.Entry<JTypeConstraint, JConstraintResult.Status>> branchIterator() {
        return null;
    }
}
