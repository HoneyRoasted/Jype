package honeyroasted.jype.system.solver.operations;

import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintMapperApplier;
import honeyroasted.almonds.ConstraintSolver;
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
import honeyroasted.jype.system.solver.constraints.reduction.ReduceCompatible;
import honeyroasted.jype.system.solver.constraints.reduction.ReduceContains;
import honeyroasted.jype.system.solver.constraints.reduction.ReduceDelayedExpressionCompatible;
import honeyroasted.jype.system.solver.constraints.reduction.ReduceEqual;
import honeyroasted.jype.system.solver.constraints.reduction.ReduceInstantiation;
import honeyroasted.jype.system.solver.constraints.reduction.ReduceMethodInvocation;
import honeyroasted.jype.system.solver.constraints.reduction.ReduceSimplyTypedExpression;
import honeyroasted.jype.system.solver.constraints.reduction.ReduceSubtype;
import honeyroasted.jype.system.visitor.visitors.MetaVarTypeResolver;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class TypeOperationsImpl implements TypeOperations {
    public static ConstraintMapperApplier COMPATIBILITY_APPLIER = new ConstraintMapperApplier(List.of(
            new ConstraintMapper.True(),
            new ConstraintMapper.False(),

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
            new ConstraintMapper.True(),
            new ConstraintMapper.False(),

            new IncorporationEqualEqual(),
            new IncorporationEqualSubtype(),
            new IncorporationSubtypeSubtype(),
            new IncorporationCapture()
    ));

    public static ConstraintMapperApplier REDUCTION_APPLIER = new ConstraintMapperApplier(List.of(
            new ConstraintMapper.True(),
            new ConstraintMapper.False(),

            new ReduceSubtype(),
            new ReduceCompatible(),
            new ReduceSimplyTypedExpression(),
            new ReduceInstantiation(),
            new ReduceMethodInvocation(),
            new ReduceDelayedExpressionCompatible(),
            //TODO implement non-simply typed expressions
            new ReduceContains(),
            new ReduceEqual(),

            new IncorporationEqualEqual(),
            new IncorporationEqualSubtype(),
            new IncorporationSubtypeSubtype(),
            new IncorporationCapture()
    ));

    public static ConstraintMapperApplier BUILD_INITIAL_BOUNDS_APPLIER = new ConstraintMapperApplier(List.of(
            new BuildInitialBounds()
    ));

    public static ConstraintMapperApplier RESOLUTION_APPLIER = new ConstraintMapperApplier(List.of(
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
                new ConstraintMapperApplier(
                        List.of(
                                BUILD_INITIAL_BOUNDS_APPLIER,
                                INCORPORATION_APPLIER,
                                REDUCTION_APPLIER,
                                BUILD_INITIAL_BOUNDS_APPLIER,
                                INCORPORATION_APPLIER,
                                RESOLUTION_APPLIER
                        )
                )
        )).withContext(new PropertySet()
                .attach(this.typeSystem)
                .attach(varTypeMapper()));
    }

    @Override
    public TypeConstraints.TypeMapper varTypeMapper() {
        return new TypeConstraints.TypeMapper(bounds -> {
            Map<VarType, MetaVarType> metaVars = new LinkedHashMap<>();

            bounds.constraints().forEach((con, status) -> {
                if (con instanceof TypeConstraints.Infer inf) {
                    metaVars.put(inf.right(), inf.left());
                }
            });

            if (metaVars.isEmpty()) {
                return Function.identity();
            } else {
                VarTypeResolveVisitor resolver = new VarTypeResolveVisitor(metaVars);
                return resolver::visit;
            }
        });
    }

    @Override
    public TypeConstraints.TypeMapper metaVarTypeMapper() {
        return new TypeConstraints.TypeMapper(bounds -> {
            Map<MetaVarType, Type> instantiations = new LinkedHashMap<>();

            bounds.constraints().forEach((con, status) -> {
                if (con instanceof TypeConstraints.Instantiation inst && status.isTrue()) {
                    instantiations.put(inst.left(), inst.right());
                }
            });

            if (instantiations.isEmpty()) {
                return Function.identity();
            } else {
                MetaVarTypeResolver resolver = new MetaVarTypeResolver(instantiations);
                return resolver::visit;
            }
        });
    }

    @Override
    public boolean isCompatible(Type subtype, Type supertype, TypeConstraints.Compatible.Context ctx) {
        return this.compatibilitySolver()
                .bind(new TypeConstraints.Compatible(subtype, ctx, supertype))
                .solve(new PropertySet().attach(subtype.typeSystem()))
                .status().isTrue();
    }

    @Override
    public boolean isSubtype(Type subtype, Type supertype) {
        return this.compatibilitySolver()
                .bind(new TypeConstraints.Subtype(subtype, supertype))
                .solve(new PropertySet().attach(subtype.typeSystem()))
                .status().isTrue();
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
