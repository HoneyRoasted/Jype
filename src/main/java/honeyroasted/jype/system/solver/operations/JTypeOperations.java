package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.tracker.JConstraintStatusTracker;
import honeyroasted.jype.system.solver.constraints.tracker.JConstraintTracker;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JType;

import java.util.Optional;
import java.util.Set;

public interface JTypeOperations {
    JTypeSystem system();

    Set<JType> findAllKnownSupertypes(JType type);

    JType findGreatestLowerBound(Set<JType> types);

    JType findLeastUpperBound(Set<JType> types);

    JType findMostSpecificType(Set<JType> types);

    Set<JType> findMostSpecificTypes(Set<JType> types);

    Optional<JClassType> outerTypeFromDeclaring(JClassReference instance, JClassReference declaring);

    default boolean isSubtype(JType left, JType right) {
        JConstraintTracker tracker = new JConstraintStatusTracker();
        checkSubtype(left, right, tracker);
        return tracker.status().isTrue();
    }

    void checkSubtype(JType left, JType right, JConstraintTracker tracker);

    default boolean isCompatible(JType left, JType right, JTypeConstraints.Compatible.Context context) {
        JConstraintTracker tracker = new JConstraintStatusTracker();
        checkCompatible(left, right, context, tracker);
        return tracker.status().isTrue();
    }

    void checkCompatible(JType left, JType right, JTypeConstraints.Compatible.Context context, JConstraintTracker tracker);
}
