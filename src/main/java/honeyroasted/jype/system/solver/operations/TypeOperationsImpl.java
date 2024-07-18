package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.TypeBoundCompoundUnwrapper;
import honeyroasted.jype.system.solver.bounds.TypeBoundMapper;
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
import honeyroasted.jype.type.Type;

import java.util.List;
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

    public static final TypeOperation<Type, Set<Type>> FIND_ALL_KNOWN_SUPERTYPES = new FindAllKnownSupertypes();
    public static final TypeOperation<Set<Type>, Type> FIND_GREATEST_LOWER_BOUND = new FindGreatestLowerBound();
    public static final TypeOperation<Set<Type>, Type> FIND_LEAST_UPPER_BOUND = new FindLeastUpperBound();
    public static final TypeOperation<Set<Type>, Type> FIND_MOST_SPECIFIC_TYPE = new FindMostSpecificType();
    public static final TypeOperation<Set<Type>, Set<Type>> FIND_MOST_SPECIFIC_TYPES = new FindMostSpecificTypes();
    public static TypeBoundMapper UPDATE_META_VARS = new UpdateMetaVars();


    private TypeSystem typeSystem;

    public TypeOperationsImpl(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public TypeSolver noOpSolver() {
        return new NoOpTypeSolver();
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
    public List<TypeBound.Result.Builder> updateMetaVars(List<TypeBound.Result.Builder> constraints) {
        return UPDATE_META_VARS.map(constraints);
    }




}
