package honeyroasted.jype.system.solver.operations;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintMapperApplier;
import honeyroasted.almonds.ConstraintSolver;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.TypeContext;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface TypeOperations {
    default ConstraintSolver noOpSolver() {
        return new ConstraintSolver(Collections.emptyList());
    }

    TypeSystem system();

    ConstraintMapperApplier reductionApplier();

    ConstraintMapperApplier incorporationApplier();

    ConstraintMapperApplier compatibilityApplier();

    ConstraintMapperApplier initialBoundsApplier();

    ConstraintSolver compatibilitySolver();

    ConstraintSolver inferenceSolver();

    TypeContext.TypeMapper varTypeMapper();

    TypeContext.TypeMapper metaVarTypeMapper();

    default ConstraintSolver inferenceSolver(Map<VarType, MetaVarType> correspondence) {
        ConstraintSolver solver = this.inferenceSolver();
        correspondence.forEach((vt, mvt) -> solver.bind(new TypeConstraints.Infer(mvt, vt)));
        return solver;
    }

    Constraint.Status checkStatus(Constraint constraint, PropertySet context);

    default boolean isCompatible(Type subtype, Type supertype, TypeConstraints.Compatible.Context ctx) {
        return checkStatus(new TypeConstraints.Compatible(subtype, ctx, supertype), new PropertySet().attach(system())).isTrue();
    }

    default boolean isSubtype(Type subtype, Type supertype) {
        return checkStatus(new TypeConstraints.Subtype(subtype, supertype), new PropertySet().attach(system())).isTrue();
    }

    Set<Type> findAllKnownSupertypes(Type type);

    Type findGreatestLowerBound(Set<Type> types);

    Type findLeastUpperBound(Set<Type> types);

    Type findMostSpecificType(Set<Type> types);

    Set<Type> findMostSpecificTypes(Set<Type> types);

    Optional<ClassType> outerTypeFromDeclaring(ClassReference instance, ClassReference declaring);
}
