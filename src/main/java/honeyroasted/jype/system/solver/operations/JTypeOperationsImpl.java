package honeyroasted.jype.system.solver.operations;

import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintMapperApplier;
import honeyroasted.almonds.ConstraintSolver;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeCompatibility;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.solver.constraints.incorporation.JIncorporationCapture;
import honeyroasted.jype.system.solver.constraints.incorporation.JIncorporationEqualEqual;
import honeyroasted.jype.system.solver.constraints.incorporation.JIncorporationEqualSubtype;
import honeyroasted.jype.system.solver.constraints.incorporation.JIncorporationSubtypeSubtype;
import honeyroasted.jype.system.solver.constraints.inference.JBuildInitialBounds;
import honeyroasted.jype.system.solver.constraints.inference.JResolveBounds;
import honeyroasted.jype.system.solver.constraints.inference.JVerifyBounds;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceCompatible;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceContains;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceEqual;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceInstantiation;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceMethodInvocation;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceMultiExpression;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceSimplyTypedExpression;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceSubtype;
import honeyroasted.jype.system.solver.constraints.tracker.JConstraintTracker;
import honeyroasted.jype.system.visitor.visitors.JMetaVarTypeResolver;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class JTypeOperationsImpl implements JTypeOperations {
    public static final ConstraintMapperApplier INCORPORATION_APPLIER = new ConstraintMapperApplier(List.of(
            new ConstraintMapper.True(),
            new ConstraintMapper.False(),

            new JIncorporationEqualEqual(),
            new JIncorporationEqualSubtype(),
            new JIncorporationSubtypeSubtype(),
            new JIncorporationCapture()
    ));

    public static final ConstraintMapperApplier REDUCTION_APPLIER = new ConstraintMapperApplier(List.of(
            new ConstraintMapper.True(),
            new ConstraintMapper.False(),

            new JReduceSubtype(),
            new JReduceCompatible(),
            new JReduceSimplyTypedExpression(),
            new JReduceMultiExpression(),
            new JReduceInstantiation(),
            new JReduceMethodInvocation(),
            //TODO implement non-simply typed expressions
            new JReduceContains(),
            new JReduceEqual(),

            new JIncorporationEqualEqual(),
            new JIncorporationEqualSubtype(),
            new JIncorporationSubtypeSubtype(),
            new JIncorporationCapture()
    ));

    public static final ConstraintMapperApplier BUILD_INITIAL_BOUNDS_APPLIER = new ConstraintMapperApplier(List.of(new JBuildInitialBounds()));

    public static final ConstraintMapperApplier INFERENCE_APPLIER = new ConstraintMapperApplier(List.of(
            new ConstraintMapper.True(),
            new ConstraintMapper.False(),

            new JBuildInitialBounds(),

            new JIncorporationEqualEqual(),
            new JIncorporationEqualSubtype(),
            new JIncorporationSubtypeSubtype(),
            new JIncorporationCapture(),

            new JReduceSubtype(),
            new JReduceCompatible(),
            new JReduceSimplyTypedExpression(),
            new JReduceMultiExpression(),
            new JReduceInstantiation(),
            new JReduceMethodInvocation(),
            //TODO implement non-simply typed expressions
            new JReduceContains(),
            new JReduceEqual(),

            new JIncorporationEqualEqual(),
            new JIncorporationEqualSubtype(),
            new JIncorporationSubtypeSubtype(),
            new JIncorporationCapture()
    ));

    public static final ConstraintMapperApplier RESOLVE_APPLIER = new ConstraintMapperApplier(List.of(
            new JResolveBounds()
    ));

    public static final ConstraintMapperApplier VERIFY_APPLIER = new ConstraintMapperApplier(List.of(
            new JVerifyBounds()
    ));

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
    public ConstraintMapperApplier reductionApplier() {
        return REDUCTION_APPLIER;
    }

    @Override
    public ConstraintMapperApplier resolutionApplier() {
        return RESOLVE_APPLIER;
    }

    @Override
    public ConstraintMapperApplier verifyApplier() {
        return VERIFY_APPLIER;
    }

    @Override
    public ConstraintMapperApplier incorporationApplier() {
        return INCORPORATION_APPLIER;
    }

    @Override
    public ConstraintMapperApplier initialBoundsApplier() {
        return BUILD_INITIAL_BOUNDS_APPLIER;
    }

    @Override
    public ConstraintSolver inferenceSolver() {
        return new ConstraintSolver(List.of(
                INFERENCE_APPLIER,
                RESOLVE_APPLIER,
                VERIFY_APPLIER
        )).withContext(new PropertySet()
                .attach(this.typeSystem)
                .attach(varTypeMapper()));
    }

    @Override
    public JTypeContext.JTypeMapper varTypeMapper() {
        return new JTypeContext.JTypeMapper(bounds -> {
            Map<JVarType, JMetaVarType> metaVars = new LinkedHashMap<>();

            bounds.constraints().forEach((con, status) -> {
                if (con instanceof JTypeConstraints.Infer inf) {
                    metaVars.put(inf.right(), inf.left());
                }
            });

            if (metaVars.isEmpty()) {
                return Function.identity();
            } else {
                JVarTypeResolveVisitor resolver = new JVarTypeResolveVisitor(metaVars);
                return resolver::visit;
            }
        });
    }

    @Override
    public JTypeContext.JTypeMapper metaVarTypeMapper() {
        return new JTypeContext.JTypeMapper(bounds -> {
            Map<JMetaVarType, JType> instantiations = new LinkedHashMap<>();

            bounds.constraints().forEach((con, status) -> {
                if (con instanceof JTypeConstraints.Instantiation inst && status.isTrue()) {
                    instantiations.put(inst.left(), inst.right());
                }
            });

            if (instantiations.isEmpty()) {
                return Function.identity();
            } else {
                JMetaVarTypeResolver resolver = new JMetaVarTypeResolver(instantiations);
                return resolver::visit;
            }
        });
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
