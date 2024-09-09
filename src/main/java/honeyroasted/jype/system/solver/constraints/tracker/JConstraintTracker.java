package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.almonds.Constraint;

import java.util.function.Consumer;

public interface JConstraintTracker {

    JConstraintTracker or(Consumer<JConstraintTracker>... actions);

    JConstraintTracker and(Consumer<JConstraintTracker>... actions);

    JConstraintTracker then(Consumer<JConstraintTracker> cons);

    default JConstraintTracker with(Constraint constraint, JConstraintResult.Status value) {
        return with(constraint).with(value);
    }

    default JConstraintTracker with(Constraint constraint, boolean value) {
        return with(constraint).with(value);
    }

    default JConstraintTracker with(boolean value) {
        return with(JConstraintResult.Status.known(value));
    }

    JConstraintTracker with(Constraint constraint);

    JConstraintTracker with(JConstraintResult.Status value);

    JConstraintTracker parent();

    boolean canChange();

    JConstraintResult.Status status();

    JConstraintResult result();

}
