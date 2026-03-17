package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.multi.Pair;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JExpressionInformation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JReduceInvocationRefExpression extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown()
                && constraint.left() instanceof JExpressionInformation.InvocationReference
                && constraint.right() instanceof JClassType;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.InvocationReference<?> methodRef =
                (JExpressionInformation.InvocationReference<?>) constraint.left();
        JClassType targetType = (JClassType) constraint.right();
        JTypeSystem system = targetType.typeSystem();

        // T must be a functional interface
        if (!targetType.classReference().hasModifier(AccessFlag.INTERFACE)) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        Optional<JMethodReference> samOpt = JReduceLambdaExpression.findFunctionalMethod(targetType);
        if (samOpt.isEmpty()) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        JMethodReference sam = samOpt.get();
        if (targetType instanceof JParameterizedClassType pct) {
            sam = (JMethodReference) pct.varTypeResolver().visit(sam);
        }

        List<JType> samParams = sam.parameters();
        JType samReturn = sam.returnType();
        boolean returnsVoid = samReturn.typeEquals(system.constants().voidType());

        List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();

        if (methodRef.source() instanceof JClassReference classRef) {
            // Static or unbound instance method reference: Type::method
            for (JMethodReference candidate : JReduceMethodInvocation.getAllMethods(classRef)) {
                if (!candidate.location().name().equals(methodRef.methodName())) continue;

                Pair<Set<Constraint>, JVarTypeResolveVisitor> tp =
                        createTypeParams(candidate, methodRef.explicitTypeArguments());
                JVarTypeResolveVisitor resolve = tp.right();

                List<JType> cParams = candidate.parameters().stream()
                        .map(resolve::apply).toList();
                JType cReturn = resolve.apply(candidate.returnType());

                // JLS §18.2.1 n=k: static method, SAM params match candidate params directly
                if (candidate.hasModifier(AccessFlag.STATIC) && cParams.size() == samParams.size()) {
                    newChildren.add(createBoundRefBranch(
                            tp.left(), samParams, cParams, cReturn, samReturn, returnsVoid, constraint));
                }

                // JLS §18.2.1 n=k+1: unbound instance method, first SAM param is the receiver
                if (!candidate.hasModifier(AccessFlag.STATIC)
                        && !candidate.hasModifier(AccessFlag.PRIVATE)
                        && samParams.size() == cParams.size() + 1) {
                    newChildren.add(createUnboundRefBranch(
                            tp.left(), classRef, samParams, cParams, cReturn, samReturn, returnsVoid, constraint));
                }
            }
        } else if (methodRef.source() instanceof JExpressionInformation sourceExpr
                && sourceExpr.isSimplyTyped()) {
            // Bound instance method reference: expr::method
            JType sourceType = sourceExpr.getSimpleType(system,
                    branchContext.firstOr(JTypeContext.TypeMetavarMap.class, JTypeContext.TypeMetavarMap.empty())).get();

            for (JClassType ct : JReduceMethodInvocation.findClassTypes(sourceType)) {
                for (JMethodReference candidate : JReduceMethodInvocation.getAllMethods(ct.classReference())) {
                    if (!candidate.location().name().equals(methodRef.methodName())) continue;
                    if (candidate.hasModifier(AccessFlag.STATIC)
                            || candidate.hasModifier(AccessFlag.PRIVATE)) continue;

                    JMethodReference resolved = candidate;
                    if (ct instanceof JParameterizedClassType pct) {
                        resolved = (JMethodReference) pct.varTypeResolver().visit(candidate);
                    }

                    Pair<Set<Constraint>, JVarTypeResolveVisitor> tp =
                            createTypeParams(resolved, methodRef.explicitTypeArguments());
                    JVarTypeResolveVisitor resolve = tp.right();

                    List<JType> cParams = resolved.parameters().stream()
                            .map(resolve::apply).toList();
                    JType cReturn = resolve.apply(resolved.returnType());

                    if (cParams.size() == samParams.size()) {
                        newChildren.add(createBoundRefBranch(
                                tp.left(), samParams, cParams, cReturn, samReturn, returnsVoid, constraint));
                    }
                }
            }
        }

        if (!newChildren.isEmpty()) {
            branch.drop(constraint).divergeBranches(newChildren);
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }

    /**
     * Branch for bound or static method references (JLS §18.2.1 n=k case):
     * SAM params match candidate params directly.
     */
    private static ConstraintBranch.Snapshot createBoundRefBranch(
            Set<Constraint> typeParamConstraints,
            List<JType> samParams, List<JType> candidateParams,
            JType candidateReturn, JType samReturn, boolean returnsVoid,
            JTypeConstraints.ExpressionCompatible constraint) {
        Map<Constraint, Constraint.Status> constraints = new HashMap<>();
        typeParamConstraints.forEach(c -> constraints.put(c, Constraint.Status.UNKNOWN));

        for (int i = 0; i < samParams.size(); i++) {
            constraints.put(
                    new JTypeConstraints.Compatible(samParams.get(i), constraint.middle(), candidateParams.get(i)),
                    Constraint.Status.UNKNOWN);
        }

        if (!returnsVoid) {
            constraints.put(
                    new JTypeConstraints.Compatible(candidateReturn, constraint.middle(), samReturn),
                    Constraint.Status.UNKNOWN);
        }

        return new ConstraintBranch.Snapshot(new PropertySet(), constraints);
    }

    /**
     * Branch for unbound instance method references (JLS §18.2.1 n=k+1 case):
     * First SAM param is the receiver, remaining params match candidate params.
     */
    private static ConstraintBranch.Snapshot createUnboundRefBranch(
            Set<Constraint> typeParamConstraints,
            JClassReference classRef,
            List<JType> samParams, List<JType> candidateParams,
            JType candidateReturn, JType samReturn, boolean returnsVoid,
            JTypeConstraints.ExpressionCompatible constraint) {
        Map<Constraint, Constraint.Status> constraints = new HashMap<>();
        typeParamConstraints.forEach(c -> constraints.put(c, Constraint.Status.UNKNOWN));

        // First SAM param <: ReferenceType (receiver type check)
        constraints.put(new JTypeConstraints.Subtype(samParams.get(0), classRef), Constraint.Status.UNKNOWN);

        // Remaining SAM params → candidate params
        for (int i = 0; i < candidateParams.size(); i++) {
            constraints.put(
                    new JTypeConstraints.Compatible(samParams.get(i + 1), constraint.middle(), candidateParams.get(i)),
                    Constraint.Status.UNKNOWN);
        }

        if (!returnsVoid) {
            constraints.put(
                    new JTypeConstraints.Compatible(candidateReturn, constraint.middle(), samReturn),
                    Constraint.Status.UNKNOWN);
        }

        return new ConstraintBranch.Snapshot(new PropertySet(), constraints);
    }

    private static Pair<Set<Constraint>, JVarTypeResolveVisitor> createTypeParams(
            JMethodReference ref, List<JArgumentType> explicitTypeArguments) {
        Set<Constraint> newConstraints = new LinkedHashSet<>();
        Map<JVarType, JType> resolution = new LinkedHashMap<>();

        if (explicitTypeArguments == null || explicitTypeArguments.isEmpty()
                || explicitTypeArguments.size() != ref.typeParameters().size()) {
            for (JVarType vt : ref.typeParameters()) {
                JMetaVarType mvt = vt.createMetaVar();
                newConstraints.add(new JTypeConstraints.Infer(mvt, vt));
                resolution.put(vt, mvt);
            }
        } else {
            for (int i = 0; i < ref.typeParameters().size(); i++) {
                resolution.put(ref.typeParameters().get(i), explicitTypeArguments.get(i));
            }
        }

        return Pair.of(newConstraints, new JVarTypeResolveVisitor(resolution));
    }
}
