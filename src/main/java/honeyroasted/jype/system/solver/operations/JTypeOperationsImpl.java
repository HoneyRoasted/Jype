package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeCompatibility;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.tracker.JConstraintTracker;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JType;

import java.util.Optional;
import java.util.Set;

public class JTypeOperationsImpl implements JTypeOperations {
    public static final JTypeOperation<JType, Set<JType>> FIND_ALL_KNOWN_SUPERTYPES = new JFindAllKnownSupertypes();
    public static final JTypeOperation<Set<JType>, JType> FIND_GREATEST_LOWER_BOUND = new JFindGreatestLowerBound();
    public static final JTypeOperation<Set<JType>, JType> FIND_LEAST_UPPER_BOUND = new JFindLeastUpperBound();
    public static final JTypeOperation<Set<JType>, JType> FIND_MOST_SPECIFIC_TYPE = new JFindMostSpecificType();
    public static final JTypeOperation<Set<JType>, Set<JType>> FIND_MOST_SPECIFIC_TYPES = new JFindMostSpecificTypes();

    private JTypeSystem typeSystem;

    public JTypeOperationsImpl(JTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public JTypeSystem system() {
        return this.typeSystem;
    }

    @Override
    public Set<JType> findAllKnownSupertypes(JType type) {
        return FIND_ALL_KNOWN_SUPERTYPES.apply(this.typeSystem, type);
    }

    @Override
    public JType findGreatestLowerBound(Set<JType> types) {
        return FIND_GREATEST_LOWER_BOUND.apply(this.typeSystem, types);
    }

    @Override
    public JType findLeastUpperBound(Set<JType> types) {
        return FIND_LEAST_UPPER_BOUND.apply(this.typeSystem, types);
    }

    @Override
    public JType findMostSpecificType(Set<JType> types) {
        return FIND_MOST_SPECIFIC_TYPE.apply(this.typeSystem, types);
    }

    @Override
    public Set<JType> findMostSpecificTypes(Set<JType> types) {
        return FIND_MOST_SPECIFIC_TYPES.apply(this.typeSystem, types);
    }

    @Override
    public Optional<JClassType> outerTypeFromDeclaring(JClassReference instance, JClassReference declaring) {
        if (instance.hasRelevantOuterType()) {
            JClassReference target = instance.outerClass();

            JClassType current = declaring;
            while (current != null && !isSubtype(current.classReference(), target)) {
                current = current.outerType();
            }
            return Optional.ofNullable(current);
        }
        return Optional.empty();
    }

    @Override
    public void checkSubtype(JType left, JType right, JConstraintTracker tracker) {
        JTypeCompatibility.checkSubtype(left, right, tracker);
    }

    @Override
    public void checkCompatible(JType left, JType right, JTypeConstraints.Compatible.Context context, JConstraintTracker tracker) {
        JTypeCompatibility.checkCompatible(left, right, context, tracker);
    }


}
