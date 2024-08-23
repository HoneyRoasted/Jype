package honeyroasted.jype.system.solver.operations;

import honeyroasted.almonds.solver.ConstraintMapperApplier;
import honeyroasted.almonds.solver.ConstraintSolver;
import honeyroasted.almonds.solver.mappers.FalseConstraintMapper;
import honeyroasted.almonds.solver.mappers.TrueConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.compatibility.CompatibleExplicitCast;
import honeyroasted.jype.system.solver.constraints.compatibility.CompatibleLooseInvocation;
import honeyroasted.jype.system.solver.constraints.compatibility.CompatibleStrictInvocation;
import honeyroasted.jype.system.solver.constraints.compatibility.EqualType;
import honeyroasted.jype.system.solver.constraints.compatibility.ExpressionAssignmentConstant;
import honeyroasted.jype.system.solver.constraints.compatibility.ExpressionSimplyTyped;
import honeyroasted.jype.system.solver.constraints.compatibility.SubtypeArray;
import honeyroasted.jype.system.solver.constraints.compatibility.SubtypeEquality;
import honeyroasted.jype.system.solver.constraints.compatibility.SubtypeGenericClass;
import honeyroasted.jype.system.solver.constraints.compatibility.SubtypeIntersection;
import honeyroasted.jype.system.solver.constraints.compatibility.SubtypeNone;
import honeyroasted.jype.system.solver.constraints.compatibility.SubtypePrimitive;
import honeyroasted.jype.system.solver.constraints.compatibility.SubtypeRawClass;
import honeyroasted.jype.system.solver.constraints.compatibility.SubtypeUnchecked;
import honeyroasted.jype.system.solver.constraints.compatibility.SubtypeVar;
import honeyroasted.jype.system.solver.constraints.compatibility.SubtypeWild;
import honeyroasted.jype.system.solver.constraints.incorporation.IncorporationCapture;
import honeyroasted.jype.system.solver.constraints.incorporation.IncorporationEqualEqual;
import honeyroasted.jype.system.solver.constraints.incorporation.IncorporationEqualSubtype;
import honeyroasted.jype.system.solver.constraints.incorporation.IncorporationSubtypeSubtype;
import honeyroasted.jype.system.solver.constraints.inference.BuildInitialBounds;
import honeyroasted.jype.system.solver.constraints.inference.ResolveBounds;
import honeyroasted.jype.system.solver.constraints.inference.VarTypeMapper;
import honeyroasted.jype.system.solver.constraints.reduction.ReduceCompatible;
import honeyroasted.jype.system.solver.constraints.reduction.ReduceContains;
import honeyroasted.jype.system.solver.constraints.reduction.ReduceEqual;
import honeyroasted.jype.system.solver.constraints.reduction.ReduceSimplyTypedExpression;
import honeyroasted.jype.system.solver.constraints.reduction.ReduceSubtype;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.Type;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TypeOperationsImpl implements TypeOperations {
    public static ConstraintMapperApplier COMPATIBILITY_APPLIER = new ConstraintMapperApplier(List.of(
            TrueConstraintMapper.INSTANCE,
            FalseConstraintMapper.INSTANCE,

            new VarTypeMapper(),

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
            new SubtypeWild(),
            new SubtypeIntersection()
    ));

    public static ConstraintMapperApplier INCORPORATION_APPLIER = new ConstraintMapperApplier(List.of(
            TrueConstraintMapper.INSTANCE,
            FalseConstraintMapper.INSTANCE,

            new VarTypeMapper(),

            new IncorporationEqualEqual(),
            new IncorporationEqualSubtype(),
            new IncorporationSubtypeSubtype(),
            new IncorporationCapture()
    ));

    public static ConstraintMapperApplier REDUCTION_APPLIER = new ConstraintMapperApplier(List.of(
            TrueConstraintMapper.INSTANCE,
            FalseConstraintMapper.INSTANCE,

            new VarTypeMapper(),

            new ReduceSubtype(),
            new ReduceCompatible(),
            new ReduceSimplyTypedExpression(),
            //TODO implement non-simply typed expressions
            new ReduceContains(),
            new ReduceEqual(),

            new IncorporationEqualEqual(),
            new IncorporationEqualSubtype(),
            new IncorporationSubtypeSubtype(),
            new IncorporationCapture()
    ));

    public static ConstraintMapperApplier BUILD_INITIAL_BOUNDS_APPLIER = new ConstraintMapperApplier(List.of(
            new VarTypeMapper(),
            new BuildInitialBounds()
    ));

    public static ConstraintMapperApplier RESOLUTION_APPLIER = new ConstraintMapperApplier(List.of(
            new VarTypeMapper(),
            new ResolveBounds()
    ));

    public static final TypeOperation<Type, Set<Type>> FIND_ALL_KNOWN_SUPERTYPES = new FindAllKnownSupertypes();
    public static final TypeOperation<Set<Type>, Type> FIND_GREATEST_LOWER_BOUND = new FindGreatestLowerBound();
    public static final TypeOperation<Set<Type>, Type> FIND_LEAST_UPPER_BOUND = new FindLeastUpperBound();
    public static final TypeOperation<Set<Type>, Type> FIND_MOST_SPECIFIC_TYPE = new FindMostSpecificType();
    public static final TypeOperation<Set<Type>, Set<Type>> FIND_MOST_SPECIFIC_TYPES = new FindMostSpecificTypes();

    private TypeSystem typeSystem;

    public TypeOperationsImpl(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public ConstraintMapperApplier reductionApplier() {
        return REDUCTION_APPLIER;
    }

    @Override
    public ConstraintMapperApplier incorporationApplier() {
        return INCORPORATION_APPLIER;
    }

    @Override
    public ConstraintMapperApplier compatibilityApplier() {
        return COMPATIBILITY_APPLIER;
    }

    @Override
    public ConstraintMapperApplier initialBoundsApplier() {
        return BUILD_INITIAL_BOUNDS_APPLIER;
    }

    @Override
    public ConstraintSolver compatibilitySolver() {
        return new ConstraintSolver(List.of(COMPATIBILITY_APPLIER));
    }

    @Override
    public ConstraintSolver inferenceSolver() {
        return new ConstraintSolver(List.of(
                BUILD_INITIAL_BOUNDS_APPLIER,
                REDUCTION_APPLIER,
                INCORPORATION_APPLIER,
                RESOLUTION_APPLIER
        )).withContext(new PropertySet().attach(this.typeSystem));
    }

    @Override
    public boolean isCompatible(Type subtype, Type supertype, TypeConstraints.Compatible.Context ctx) {
        return this.compatibilitySolver()
                .bind(new TypeConstraints.Compatible(subtype, ctx, supertype))
                .solve(new PropertySet().attach(subtype.typeSystem()))
                .success();
    }

    @Override
    public boolean isSubtype(Type subtype, Type supertype) {
        return this.compatibilitySolver()
                .bind(new TypeConstraints.Subtype(subtype, supertype))
                .solve(new PropertySet().attach(subtype.typeSystem()))
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
    public Optional<ClassType> outerTypeFromDeclaring(ClassReference instance, ClassReference declaring) {
        if (instance.hasRelevantOuterType()) {
            ClassReference target = instance.outerClass();

            ClassType current = declaring;
            while (current != null && !isSubtype(current.classReference(), target)) {
                current = current.outerType();
            }
            return Optional.ofNullable(current);
        }
        return Optional.empty();
    }


}
