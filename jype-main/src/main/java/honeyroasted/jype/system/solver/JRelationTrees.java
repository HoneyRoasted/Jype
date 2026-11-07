package honeyroasted.jype.system.solver;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;

public final class JRelationTrees {
    private JRelationTrees() {
    }

    public static boolean isRelation(Constraint constraint) {
        return switch (constraint) {
            case JTypeConstraints.Subtype c -> c.left().isProperType() && c.right().isProperType();
            case JTypeConstraints.Contains c -> c.left().isProperType() && c.right().isProperType();
            case JTypeConstraints.Compatible c -> c.left().isProperType() && c.right().isProperType();
            case JTypeConstraints.Equal c -> c.left().isProperType() && c.right().isProperType();
            default -> false;
        };
    }

    public static boolean ask(ConstraintBranch branch, Constraint constraint, JSolverConfig config) {
        if (!config.relationTrees()) {
            boolean result = evaluate(constraint, config.untrackedLimit());
            branch.set(constraint, Constraint.Status.known(result));
            return result;
        }
        JConstraintTreeTracker tracker = new JConstraintTreeTracker(config.maxRelationDepth());
        boolean result = evaluate(constraint, tracker);
        tracker.apply(branch, constraint, result, config.failedAttempts());
        return result;
    }

    private static boolean evaluate(Constraint constraint, JRelationTracker tracker) {
        return switch (constraint) {
            case JTypeConstraints.Subtype c -> JTypeRelations.isSubtype(c.left(), c.right(), tracker);
            case JTypeConstraints.Contains c -> JTypeRelations.contains(c.left(), c.right(), tracker);
            case JTypeConstraints.Compatible c -> JTypeRelations.isCompatible(c.left(), c.middle(), c.right(), tracker);
            case JTypeConstraints.Equal c -> sameType(c, tracker);
            default -> throw new IllegalArgumentException("Not a relation constraint: " + constraint);
        };
    }

    private static boolean sameType(JTypeConstraints.Equal constraint, JRelationTracker tracker) {
        boolean result = JTypeRelations.isSameType(constraint.left(), constraint.right());
        if (tracker != null) {
            tracker.enter(constraint);
            tracker.rule("JLS §4.3.4 same type");
            tracker.exit(result);
        }
        return result;
    }
}
