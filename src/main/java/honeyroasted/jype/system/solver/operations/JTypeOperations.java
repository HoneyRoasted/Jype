package honeyroasted.jype.system.solver.operations;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintSolver;
import honeyroasted.almonds.applier.ConstraintMapperApplier;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.Collections;
import java.util.HashMap;
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

    ConstraintMapperApplier compatibilityApplier();

    ConstraintMapperApplier initialBoundsApplier();

    ConstraintSolver compatibilitySolver();

    ConstraintSolver inferenceSolver();

    Map<JVarType, JMetaVarType> varTypeMap(ConstraintBranch branch);

    Map<JMetaVarType, JType> metaVarTypeMap(ConstraintBranch branch);

    default ConstraintSolver inferenceSolver(Map<JVarType, JMetaVarType> correspondence) {
        ConstraintSolver solver = this.inferenceSolver();
        JTypeContext.TypeMetavarMap metavarMap = new JTypeContext.TypeMetavarMap(new HashMap<>(), new HashMap<>(correspondence));
        correspondence.forEach((vt, mvt) -> solver.bind(new JTypeConstraints.Infer(mvt, vt)));
        return solver.withContext(new PropertySet().attach(metavarMap));
    }

    Constraint.Status checkStatus(Constraint constraint, PropertySet context);

    default boolean isCompatible(JType subtype, JType supertype, JTypeConstraints.Compatible.Context ctx) {
        return checkStatus(new JTypeConstraints.Compatible(subtype, ctx, supertype), new PropertySet().attach(system())).isTrue();
    }

    default boolean isSubtype(JType subtype, JType supertype) {
        return checkStatus(new JTypeConstraints.Subtype(subtype, supertype), new PropertySet().attach(system())).isTrue();
    }

    Set<JType> findAllKnownSupertypes(JType type);

    JType findGreatestLowerBound(Set<JType> types);

    JType findLeastUpperBound(Set<JType> types);

    JType findMostSpecificType(Set<JType> types);

    Set<JType> findMostSpecificTypes(Set<JType> types);

    Optional<JClassType> outerTypeFromDeclaring(JClassReference instance, JClassReference declaring);

}
