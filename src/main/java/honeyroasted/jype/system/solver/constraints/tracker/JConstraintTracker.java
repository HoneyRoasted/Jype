package honeyroasted.jype.system.solver.constraints.tracker;

import honeyroasted.almonds.Constraint;

import java.util.function.Consumer;

public interface JConstraintTracker {

    JConstraintTracker or(Consumer<JConstraintTracker>... actions);

    JConstraintTracker and(Consumer<JConstraintTracker>... actions);

    JConstraintTracker then(Consumer<JConstraintTracker> cons);

    default JConstraintTracker with(Constraint constraint, boolean value) {
        return with(constraint).with(value);
    }

    JConstraintTracker with(Constraint constraint);

    JConstraintTracker with(boolean value);

    JConstraintTracker parent();

    boolean canChange();

    boolean status();

    JConstraintResult result();

}
