package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.TypeBoundMapperApplier;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface TypeOperations {
    TypeSolver noOpSolver();

    TypeBoundMapperApplier incorporationApplier();

    TypeBoundMapperApplier compatibilityApplier();

    TypeSolver compatibilitySolver();

    boolean isCompatible(Type subtype, Type supertype, TypeBound.Compatible.Context ctx);

    boolean isSubtype(Type subtype, Type supertype);

    Set<Type> findAllKnownSupertypes(Type type);

    Type findGreatestLowerBound(Set<Type> types);

    Type findLeastUpperBound(Set<Type> types);

    Type findMostSpecificType(Set<Type> types);

    Set<Type> findMostSpecificTypes(Set<Type> types);

    Set<TypeBound.Result.Builder> buildInitialBounds(Map<VarType, MetaVarType> metaVars);

    Collection<TypeBound.Result.Builder> updateMetaVars(Collection<TypeBound.Result.Builder> constraints);
}
