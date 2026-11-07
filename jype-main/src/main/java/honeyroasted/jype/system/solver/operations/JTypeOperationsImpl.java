package honeyroasted.jype.system.solver.operations;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintSolver;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.almonds.applier.ConstraintMapperApplier;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.compatibility.JEqualType;
import honeyroasted.jype.system.solver.constraints.compatibility.JProperCompatible;
import honeyroasted.jype.system.solver.constraints.compatibility.JProperContains;
import honeyroasted.jype.system.solver.constraints.compatibility.JProperSubtype;
import honeyroasted.jype.system.solver.constraints.compatibility.JExpressionAssignmentConstant;
import honeyroasted.jype.system.solver.constraints.compatibility.JExpressionSimplyTyped;
import honeyroasted.jype.system.solver.constraints.incorporation.JIncorporationCapture;
import honeyroasted.jype.system.solver.constraints.incorporation.JIncorporationEqualEqual;
import honeyroasted.jype.system.solver.constraints.incorporation.JIncorporationEqualSubtype;
import honeyroasted.jype.system.solver.constraints.incorporation.JIncorporationSubtypeSubtype;
import honeyroasted.jype.system.solver.constraints.inference.JBuildInitialBounds;
import honeyroasted.jype.system.solver.constraints.inference.JResolveBounds;
import honeyroasted.jype.system.solver.constraints.inference.JResolveMostSpecificMethod;
import honeyroasted.jype.system.solver.constraints.inference.JVerifyBounds;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceArrayCreation;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceArrayInitializer;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceBinaryOperator;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceCast;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceCompatible;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceConditionalExpression;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceContains;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceEqual;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceGetField;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceInstanceOf;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceInstantiation;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceInstantiationRefExpression;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceInvocationRefExpression;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceDeferred;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceLambdaThrowsCheck;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceLambdaExpression;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceMethodInvocation;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceSimplyTypedExpression;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceSubtype;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceUnaryOperator;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedMethodType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JTypeOperationsImpl implements JTypeOperations {
    public static final List<ConstraintMapper> COMPATIBILITY_MAPPERS = List.of(
            new ConstraintMapper.True(),
            new ConstraintMapper.False(),

            new JEqualType(),
            new JProperSubtype(),
            new JProperCompatible(),
            new JProperContains(),

            new JExpressionAssignmentConstant(),
            new JExpressionSimplyTyped()
    );

    public static final List<ConstraintMapper> INCORPORATION_MAPPERS = List.of(
            new ConstraintMapper.True(),
            new ConstraintMapper.False(),

            new JIncorporationEqualEqual(),
            new JIncorporationEqualSubtype(),
            new JIncorporationSubtypeSubtype(),
            new JIncorporationCapture()
    );

    public static final List<ConstraintMapper> REDUCTION_MAPPERS = List.of(
            new ConstraintMapper.True(),
            new ConstraintMapper.False(),

            new JReduceSubtype(),
            new JReduceCompatible(),
            new JReduceContains(),
            new JReduceEqual(),

            new JReduceCast(),
            new JReduceInstanceOf(),
            new JReduceUnaryOperator(),
            new JReduceBinaryOperator(),
            new JReduceConditionalExpression(),
            new JReduceArrayCreation(),
            new JReduceArrayInitializer(),
            new JReduceSimplyTypedExpression(),
            new JReduceInstantiation(),
            new JReduceMethodInvocation(),
            new JReduceGetField(),
            new JReduceInstantiationRefExpression(),
            new JReduceInvocationRefExpression(),
            new JReduceLambdaExpression(),
            new JReduceDeferred(),
            new JReduceLambdaThrowsCheck(),

            new JIncorporationEqualEqual(),
            new JIncorporationEqualSubtype(),
            new JIncorporationSubtypeSubtype(),
            new JIncorporationCapture()
    );

    public static final List<ConstraintMapper> BUILD_INITIAL_BOUNDS_MAPPERS = List.of(
            new JBuildInitialBounds()
    );

    public static final List<ConstraintMapper> INFERENCE_MAPPERS = List.of(
            new ConstraintMapper.True(),
            new ConstraintMapper.False(),

            new JBuildInitialBounds(),

            new JIncorporationEqualEqual(),
            new JIncorporationEqualSubtype(),
            new JIncorporationSubtypeSubtype(),
            new JIncorporationCapture(),

            new JReduceSubtype(),
            new JReduceCompatible(),
            new JReduceContains(),
            new JReduceEqual(),

            new JReduceCast(),
            new JReduceInstanceOf(),
            new JReduceUnaryOperator(),
            new JReduceBinaryOperator(),
            new JReduceConditionalExpression(),
            new JReduceArrayCreation(),
            new JReduceArrayInitializer(),
            new JReduceSimplyTypedExpression(),
            new JReduceInstantiation(),
            new JReduceMethodInvocation(),
            new JReduceGetField(),
            new JReduceInstantiationRefExpression(),
            new JReduceInvocationRefExpression(),
            new JReduceLambdaExpression(),
            new JReduceDeferred(),
            new JReduceLambdaThrowsCheck(),

            new JIncorporationEqualEqual(),
            new JIncorporationEqualSubtype(),
            new JIncorporationSubtypeSubtype(),
            new JIncorporationCapture()
    );

    public static final List<ConstraintMapper> RESOLVE_MAPPERS = List.of(
            new JResolveBounds()
    );

    public static final List<ConstraintMapper> VERIFY_MAPPERS = List.of(
            new JVerifyBounds()
    );

    public static final JTypeOperation<JType, Set<JType>> FIND_ALL_KNOWN_SUPERTYPES = new JFindAllKnownSupertypes();
    public static final JTypeOperation<Set<JType>, JType> FIND_GREATEST_LOWER_BOUND = new JFindGreatestLowerBound();
    public static final JTypeOperation<Set<JType>, JType> FIND_LEAST_UPPER_BOUND = new JFindLeastUpperBound();
    public static final JTypeOperation<Set<JType>, JType> FIND_MOST_SPECIFIC_TYPE = new JFindMostSpecificType();
    public static final JTypeOperation<Set<JType>, Set<JType>> FIND_MOST_SPECIFIC_TYPES = new JFindMostSpecificTypes();
    public static final JTypeOperation<JExpressionInformation.MethodInvocation<?>, JParameterizedMethodType> FIND_APPLICABLE_METHOD = new JFindApplicableMethod();
    public static final JCheckOverrideValidity CHECK_OVERRIDE_VALIDITY = new JCheckOverrideValidity();

    private JTypeSystem typeSystem;

    public JTypeOperationsImpl(JTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public JTypeSystem system() {
        return this.typeSystem;
    }

    @Override
    public ConstraintMapperApplier reductionApplier(ConstraintMapperApplier.Type type) {
        return ConstraintMapperApplier.of(REDUCTION_MAPPERS, type);
    }

    @Override
    public ConstraintMapperApplier resolutionApplier(ConstraintMapperApplier.Type type) {
        return ConstraintMapperApplier.of(RESOLVE_MAPPERS, type);
    }

    @Override
    public ConstraintMapperApplier verifyApplier(ConstraintMapperApplier.Type type) {
        return ConstraintMapperApplier.of(VERIFY_MAPPERS, type);
    }

    @Override
    public ConstraintMapperApplier incorporationApplier(ConstraintMapperApplier.Type type) {
        return ConstraintMapperApplier.of(INCORPORATION_MAPPERS, type);
    }

    @Override
    public ConstraintMapperApplier compatibilityApplier(ConstraintMapperApplier.Type type) {
        return ConstraintMapperApplier.of(COMPATIBILITY_MAPPERS, type);
    }

    @Override
    public ConstraintMapperApplier initialBoundsApplier(ConstraintMapperApplier.Type type) {
        return ConstraintMapperApplier.of(BUILD_INITIAL_BOUNDS_MAPPERS, type);
    }

    @Override
    public ConstraintSolver compatibilitySolver() {
        return new ConstraintSolver(List.of(this.compatibilityApplier(ConstraintMapperApplier.Type.ORDERED)));
    }

    @Override
    public ConstraintSolver inferenceSolver() {
        JTypeSystem system = this.typeSystem;
        List<ConstraintMapper> loop = new java.util.ArrayList<>(INFERENCE_MAPPERS);
        loop.addAll(RESOLVE_MAPPERS);
        ConstraintSolver solver = new ConstraintSolver(List.of(
                ConstraintMapperApplier.of(loop, ConstraintMapperApplier.Type.UNTRIMMED),
                this.verifyApplier(ConstraintMapperApplier.Type.UNTRIMMED)
        )).postSolve(tree -> JResolveMostSpecificMethod.prune(tree, system));
        return solver.withContext(new PropertySet()
                .attach(this.typeSystem));
    }

    @Override
    public Map<JVarType, JMetaVarType> varTypeMap(ConstraintBranch branch) {
        Map<JVarType, JMetaVarType> metaVars = new LinkedHashMap<>();

        branch.constraints().forEach((con, status) -> {
            if (con instanceof JTypeConstraints.Infer inf) {
                metaVars.put(inf.right(), inf.left());
            }
        });

        return metaVars;
    }

    @Override
    public Map<JMetaVarType, JType> metaVarTypeMap(ConstraintBranch branch) {
        Map<JMetaVarType, JType> instantiations = new LinkedHashMap<>();

        branch.constraints().forEach((con, status) -> {
            if (con instanceof JTypeConstraints.Instantiation inst && status.isTrue()) {
                instantiations.put(inst.left(), inst.right());
            }
        });

        return instantiations;
    }

    @Override
    public Constraint.Status checkStatus(Constraint constraint, PropertySet context) {
        return this.compatibilitySolver()
                .bind(constraint)
                .solve(context)
                .status();
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
    public JParameterizedMethodType findApplicableMethod(JExpressionInformation.MethodInvocation<?> invocation) {
        return FIND_APPLICABLE_METHOD.apply(this.typeSystem, invocation);
    }

    @Override
    public JCheckOverrideValidity.Result checkOverrideValidity(JMethodReference override, JMethodReference overridden) {
        return CHECK_OVERRIDE_VALIDITY.apply(this.typeSystem, new JCheckOverrideValidity.Pair(override, overridden));
    }

}
