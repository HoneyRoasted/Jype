package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.jype.system.solver.constraints.JTypeConstraint;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public class JConstraintTreeTracker implements JConstraintTracker {
    private JConstraintTree head;

    @Override
    public JConstraintTracker push(JTypeConstraint constraint, JConstraintResult.Operator op) {
        return null;
    }

    @Override
    public JConstraintTracker pop() {
        return null;
    }

    @Override
    public JTypeConstraint peek() {
        return null;
    }

    @Override
    public JConstraintTracker with(JTypeConstraint constraint, JConstraintResult.Status value) {
        return null;
    }

    @Override
    public JConstraintTracker set(JConstraintResult.Status value) {
        return null;
    }

    @Override
    public JConstraintTracker then(Consumer<JConstraintTracker> cons) {
        return null;
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
