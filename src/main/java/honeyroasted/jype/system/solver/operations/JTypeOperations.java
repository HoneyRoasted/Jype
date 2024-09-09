package honeyroasted.jype.system.solver.operations;

import honeyroasted.almonds.ConstraintMapperApplier;
import honeyroasted.almonds.ConstraintSolver;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.solver.constraints.tracker.JConstraintTracker;
import honeyroasted.jype.system.solver.constraints.tracker.JStatusConstraintTracker;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface JTypeOperations {
    default ConstraintSolver noOpSolver() {
        return new ConstraintSolver(Collections.emptyList());
    }

    JTypeSystem system();

    ConstraintMapperApplier reductionApplier();

    ConstraintMapperApplier resolutionApplier();

    ConstraintMapperApplier verifyApplier();

    ConstraintMapperApplier incorporationApplier();

    ConstraintMapperApplier initialBoundsApplier();

    ConstraintSolver inferenceSolver();

    JTypeContext.JTypeMapper varTypeMapper();

    JTypeContext.JTypeMapper metaVarTypeMapper();

    default ConstraintSolver inferenceSolver(Map<JVarType, JMetaVarType> correspondence) {
        ConstraintSolver solver = this.inferenceSolver();
        correspondence.forEach((vt, mvt) -> solver.bind(new JTypeConstraints.Infer(mvt, vt)));
        return solver;
    }

    Set<JType> findAllKnownSupertypes(JType type);

    JType findGreatestLowerBound(Set<JType> types);

    JType findLeastUpperBound(Set<JType> types);

    JType findMostSpecificType(Set<JType> types);

    Set<JType> findMostSpecificTypes(Set<JType> types);

    Optional<JClassType> outerTypeFromDeclaring(JClassReference instance, JClassReference declaring);

    default boolean isSubtype(JType left, JType right) {
        JConstraintTracker tracker = new JStatusConstraintTracker();
        checkSubtype(left, right, tracker);
        return tracker.status();
    }

    void checkSubtype(JType left, JType right, JConstraintTracker tracker);

    default boolean isCompatible(JType left, JType right, JTypeConstraints.Compatible.Context context) {
        JConstraintTracker tracker = new JStatusConstraintTracker();
        checkCompatible(left, right, context, tracker);
        return tracker.status();
    }

    void checkCompatible(JType left, JType right, JTypeConstraints.Compatible.Context context, JConstraintTracker tracker);
}
