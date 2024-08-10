package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.TypeBoundCompoundUnwrapper;
import honeyroasted.jype.system.solver.bounds.TypeBoundMapperApplier;
import honeyroasted.jype.system.solver.solvers.NoOpTypeSolver;
import honeyroasted.jype.system.solver.solvers.TypeBoundMapperSolver;
import honeyroasted.jype.system.solver.solvers.compatibility.CompatibleExplicitCast;
import honeyroasted.jype.system.solver.solvers.compatibility.CompatibleLooseInvocation;
import honeyroasted.jype.system.solver.solvers.compatibility.CompatibleStrictInvocation;
import honeyroasted.jype.system.solver.solvers.compatibility.EqualType;
import honeyroasted.jype.system.solver.solvers.compatibility.ExpressionAssignmentConstant;
import honeyroasted.jype.system.solver.solvers.compatibility.ExpressionSimplyTyped;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeArray;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeEquality;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeGenericClass;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeIntersection;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeMetaVar;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeNone;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypePrimitive;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeRawClass;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeUnchecked;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeVar;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeWild;
import honeyroasted.jype.system.solver.solvers.incorporation.IncorporationCapture;
import honeyroasted.jype.system.solver.solvers.incorporation.IncorporationEqualEqual;
import honeyroasted.jype.system.solver.solvers.incorporation.IncorporationEqualSubtype;
import honeyroasted.jype.system.solver.solvers.incorporation.IncorporationSubtypeSubtype;
import honeyroasted.jype.system.solver.solvers.reduction.ReduceCompatible;
import honeyroasted.jype.system.solver.solvers.reduction.ReduceContains;
import honeyroasted.jype.system.solver.solvers.reduction.ReduceEqual;
import honeyroasted.jype.system.solver.solvers.reduction.ReduceSimplyTypedExpression;
import honeyroasted.jype.system.solver.solvers.reduction.ReduceSubtype;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeOperationsImpl implements TypeOperations {
    public static TypeBoundMapperApplier COMPATIBILITY_APPLIER = new TypeBoundMapperApplier(List.of(
            new TypeBoundCompoundUnwrapper(),

            new EqualType(),

            new CompatibleExplicitCast(),
            new CompatibleLooseInvocation(),
            new CompatibleStrictInvocation(),

            new ExpressionAssignmentConstant(),
            new ExpressionSimplyTyped(),

            new SubtypeNone(),
            new SubtypePrimitive(),
            new SubtypeEquality(),
            new SubtypeUnchecked(),
            new SubtypeRawClass(),
            new SubtypeGenericClass(),
            new SubtypeArray(),
            new SubtypeVar(),
            new SubtypeMetaVar(),
            new SubtypeWild(),
            new SubtypeIntersection()
    ));

    public static TypeBoundMapperApplier INCORPORATION_APPLIER = new TypeBoundMapperApplier(List.of(
            new TypeBoundCompoundUnwrapper(),

            new IncorporationEqualEqual(),
            new IncorporationEqualSubtype(),
            new IncorporationSubtypeSubtype(),
            new IncorporationCapture()
    ));

    public static TypeBoundMapperApplier REDUCTION_APPLIER = new TypeBoundMapperApplier(List.of(
            new TypeBoundCompoundUnwrapper(),

            new ReduceSubtype(),
            new ReduceCompatible(),
            new ReduceSimplyTypedExpression(),
            //TODO implement non-simply typed expressions
            new ReduceContains(),
            new ReduceEqual(),

            INCORPORATION_APPLIER
    ));

    public static final TypeOperation<Type, Set<Type>> FIND_ALL_KNOWN_SUPERTYPES = new FindAllKnownSupertypes();
    public static final TypeOperation<Set<Type>, Type> FIND_GREATEST_LOWER_BOUND = new FindGreatestLowerBound();
    public static final TypeOperation<Set<Type>, Type> FIND_LEAST_UPPER_BOUND = new FindLeastUpperBound();
    public static final TypeOperation<Set<Type>, Type> FIND_MOST_SPECIFIC_TYPE = new FindMostSpecificType();
    public static final TypeOperation<Set<Type>, Set<Type>> FIND_MOST_SPECIFIC_TYPES = new FindMostSpecificTypes();
    public static final TypeOperation<Map<VarType, MetaVarType>, Set<TypeBound.Result.Builder>> BUILD_INITIAL_BOUNDS = new BuildInitialBounds();
    public static final TypeOperation<TypeBound.Result.Builder, TypeBound.Result.Builder> UPDATE_META_VARS = new UpdateMetaVars();
    public static final TypeOperation<Set<TypeBound.Result.Builder>, Pair<Map<MetaVarType, Type>, Set<TypeBound.Result.Builder>>> RESOLVE_BOUNDS = new ResolveBounds();

    private TypeSystem typeSystem;

    public TypeOperationsImpl(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    public TypeSolver noOpSolver() {
        return new NoOpTypeSolver();
    }

    @Override
    public TypeBoundMapperApplier reductionApplier() {
        return REDUCTION_APPLIER;
    }

    @Override
    public TypeBoundMapperApplier incorporationApplier() {
        return INCORPORATION_APPLIER;
    }

    @Override
    public TypeBoundMapperApplier compatibilityApplier() {
        return COMPATIBILITY_APPLIER;
    }

    @Override
    public TypeSolver compatibilitySolver() {
        return new TypeBoundMapperSolver("CompatibilityTypeSolver",
                Set.of(TypeBound.Equal.class, TypeBound.Subtype.class, TypeBound.Compatible.class),
                COMPATIBILITY_APPLIER);
    }

    @Override
    public boolean isCompatible(Type subtype, Type supertype, TypeBound.Compatible.Context ctx) {
        return this.compatibilitySolver()
                .bind(new TypeBound.Compatible(subtype, supertype, ctx))
                .solve(this.typeSystem)
                .success();
    }

    @Override
    public boolean isSubtype(Type subtype, Type supertype) {
        return this.compatibilitySolver()
                .bind(new TypeBound.Subtype(subtype, supertype))
                .solve(this.typeSystem)
                .success();
    }

    @Override
    public Set<Type> findAllKnownSupertypes(Type type) {
        return FIND_ALL_KNOWN_SUPERTYPES.apply(this.typeSystem, type);
    }

    @Override
    public Type findGreatestLowerBound(Set<Type> types) {
        return FIND_GREATEST_LOWER_BOUND.apply(this.typeSystem, types);
    }

    @Override
    public Type findLeastUpperBound(Set<Type> types) {
        return FIND_LEAST_UPPER_BOUND.apply(this.typeSystem, types);
    }

    @Override
    public Type findMostSpecificType(Set<Type> types) {
        return FIND_MOST_SPECIFIC_TYPE.apply(this.typeSystem, types);
    }

    @Override
    public Set<Type> findMostSpecificTypes(Set<Type> types) {
        return FIND_MOST_SPECIFIC_TYPES.apply(this.typeSystem, types);
    }

    @Override
    public Set<TypeBound.Result.Builder> buildInitialBounds(Map<VarType, MetaVarType> metaVars) {
        return BUILD_INITIAL_BOUNDS.apply(this.typeSystem, metaVars);
    }

    @Override
    public Collection<TypeBound.Result.Builder> updateMetaVars(Collection<TypeBound.Result.Builder> constraints) {
        constraints.forEach(t -> UPDATE_META_VARS.apply(this.typeSystem, t));
        return constraints;
    }

    @Override
    public Pair<Map<MetaVarType, Type>, Set<TypeBound.Result.Builder>> resolveBounds(Set<TypeBound.Result.Builder> bounds) {
        return RESOLVE_BOUNDS.apply(this.typeSystem, bounds);
    }


}
