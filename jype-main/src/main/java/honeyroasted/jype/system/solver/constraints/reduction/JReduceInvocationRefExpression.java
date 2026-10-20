package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.multi.Pair;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.JMemberLookup;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
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

    private record ResolvedCandidate(Set<Constraint> typeParamConstraints, List<JType> params, JType returnType) {
    }

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown()
                && constraint.left() instanceof JExpressionInformation.InvocationReference
                && (constraint.right() instanceof JClassType || constraint.right() instanceof JIntersectionType);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.InvocationReference<?> methodRef =
                (JExpressionInformation.InvocationReference<?>) constraint.left();
        JType targetType = constraint.right();
        JTypeSystem system = targetType.typeSystem();

        Optional<JMethodReference> samOpt = JReduceLambdaExpression.findFunctionalMethod(targetType);
        if (samOpt.isEmpty()) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        JMethodReference sam = samOpt.get();
        List<JType> samParams = sam.parameters();
        JType samReturn = sam.returnType();
        boolean returnsVoid = samReturn.typeEquals(system.constants().voidType());

        List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();

        if (methodRef.source() instanceof JClassReference classRef) {
            // Static or unbound instance method reference: Type::method
            for (JMethodReference candidate : JMemberLookup.allMethods(classRef)) {
                if (!candidate.location().name().equals(methodRef.methodName())) continue;

                int declaredParamCount = candidate.parameters().size();

                boolean staticFixed = candidate.hasModifier(AccessFlag.STATIC) && declaredParamCount == samParams.size();
                boolean staticVararg = candidate.hasModifier(AccessFlag.STATIC) && candidate.hasModifier(AccessFlag.VARARGS) &&
                        samParams.size() >= declaredParamCount - 1 &&
                        candidate.parameters().get(declaredParamCount - 1) instanceof JArrayType;

                boolean unboundFixed = !candidate.hasModifier(AccessFlag.STATIC) && !candidate.hasModifier(AccessFlag.PRIVATE) &&
                        samParams.size() == declaredParamCount + 1;
                boolean unboundVararg = !candidate.hasModifier(AccessFlag.STATIC) && !candidate.hasModifier(AccessFlag.PRIVATE) &&
                        candidate.hasModifier(AccessFlag.VARARGS) &&
                        samParams.size() >= declaredParamCount &&
                        candidate.parameters().get(declaredParamCount - 1) instanceof JArrayType;

                // JLS §18.2.1 n=k: static method, SAM params match candidate params directly
                if (staticFixed) {
                    ResolvedCandidate rc = resolveCandidate(candidate, methodRef.explicitTypeArguments());
                    addIfPresent(newChildren, createBoundRefBranch(
                            candidate, rc.typeParamConstraints(), samParams, rc.params(), rc.returnType(), samReturn, returnsVoid, sam.exceptionTypes(), constraint));
                }

                // JLS §15.13.2 variable arity: static method, trailing SAM params spread into the vararg array
                if (staticVararg) {
                    ResolvedCandidate rc = resolveCandidate(candidate, methodRef.explicitTypeArguments());
                    addIfPresent(newChildren, createBoundVarargRefBranch(
                            candidate, rc.typeParamConstraints(), samParams, rc.params(), rc.returnType(), samReturn, returnsVoid, sam.exceptionTypes(), constraint));
                }

                // JLS §18.2.1 n=k+1: unbound instance method, first SAM param is the receiver
                if (unboundFixed) {
                    ResolvedCandidate rc = resolveCandidate(candidate, methodRef.explicitTypeArguments());
                    addIfPresent(newChildren, createUnboundRefBranch(
                            candidate, rc.typeParamConstraints(), classRef, samParams, rc.params(), rc.returnType(), samReturn, returnsVoid, sam.exceptionTypes(), constraint));
                }

                // JLS §15.13.2 variable arity: unbound instance method, remaining SAM params spread into the vararg array
                if (unboundVararg) {
                    ResolvedCandidate rc = resolveCandidate(candidate, methodRef.explicitTypeArguments());
                    addIfPresent(newChildren, createUnboundVarargRefBranch(
                            candidate, rc.typeParamConstraints(), classRef, samParams, rc.params(), rc.returnType(), samReturn, returnsVoid, sam.exceptionTypes(), constraint));
                }
            }
        } else if (methodRef.source() instanceof JExpressionInformation sourceExpr
                && sourceExpr.isSimplyTyped()) {
            // Bound instance method reference: expr::method
            JType sourceType = sourceExpr.getSimpleType(system,
                    branchContext.firstOr(JTypeContext.TypeMetavarMap.class, JTypeContext.TypeMetavarMap.empty())).get();
            sourceType = JMemberLookup.captureConvert(branch, sourceType); // JLS §5.1.10

            for (JClassType ct : JMemberLookup.findClassTypes(sourceType)) {
                for (JMethodReference candidate : JMemberLookup.allMethods(ct.classReference())) {
                    if (!candidate.location().name().equals(methodRef.methodName())) continue;
                    if (candidate.hasModifier(AccessFlag.STATIC)
                            || candidate.hasModifier(AccessFlag.PRIVATE)) continue;

                    JMethodReference resolved = candidate;
                    if (ct instanceof JParameterizedClassType pct) {
                        resolved = (JMethodReference) pct.varTypeResolver().visit(candidate);
                    }

                    int declaredParamCount = resolved.parameters().size();

                    boolean fixed = declaredParamCount == samParams.size();
                    boolean vararg = resolved.hasModifier(AccessFlag.VARARGS) &&
                            samParams.size() >= declaredParamCount - 1 &&
                            resolved.parameters().get(declaredParamCount - 1) instanceof JArrayType;

                    if (fixed) {
                        ResolvedCandidate rc = resolveCandidate(resolved, methodRef.explicitTypeArguments());
                        addIfPresent(newChildren, createBoundRefBranch(
                                resolved, rc.typeParamConstraints(), samParams, rc.params(), rc.returnType(), samReturn, returnsVoid, sam.exceptionTypes(), constraint));
                    }

                    if (vararg) {
                        ResolvedCandidate rc = resolveCandidate(resolved, methodRef.explicitTypeArguments());
                        addIfPresent(newChildren, createBoundVarargRefBranch(
                                resolved, rc.typeParamConstraints(), samParams, rc.params(), rc.returnType(), samReturn, returnsVoid, sam.exceptionTypes(), constraint));
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

    private static ResolvedCandidate resolveCandidate(JMethodReference candidate, List<JArgumentType> explicitTypeArguments) {
        Pair<Set<Constraint>, JVarTypeResolveVisitor> tp = createTypeParams(candidate, explicitTypeArguments);
        JVarTypeResolveVisitor resolve = tp.right();

        List<JType> params = candidate.parameters().stream().map(resolve::apply).toList();
        JType returnType = resolve.apply(candidate.returnType());

        return new ResolvedCandidate(tp.left(), params, returnType);
    }

    private static void addIfPresent(List<ConstraintBranch.Snapshot> children, ConstraintBranch.Snapshot snapshot) {
        if (snapshot != null) children.add(snapshot);
    }

    /**
     * Branch for bound or static method references (JLS §18.2.1 n=k case):
     * SAM params match candidate params directly.
     */
    private static ConstraintBranch.Snapshot createBoundRefBranch(
            JMethodReference candidate, Set<Constraint> typeParamConstraints,
            List<JType> samParams, List<JType> candidateParams,
            JType candidateReturn, JType samReturn, boolean returnsVoid, List<JType> samThrows,
            JTypeConstraints.ExpressionCompatible constraint) {
        Map<Constraint, Constraint.Status> constraints = new HashMap<>();
        constraints.put(Constraint.identity(Set.of(candidate, false)), Constraint.Status.TRUE);
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

        if (!JMemberLookup.addThrowsConstraints(constraints, candidate, samThrows)) return null;

        return new ConstraintBranch.Snapshot(new PropertySet(), constraints);
    }

    /**
     * Branch for variable arity static or bound method references (JLS §15.13.2):
     * leading SAM params match the candidate's fixed params directly, remaining SAM params
     * are spread into the candidate's trailing vararg array component type.
     */
    private static ConstraintBranch.Snapshot createBoundVarargRefBranch(
            JMethodReference candidate, Set<Constraint> typeParamConstraints,
            List<JType> samParams, List<JType> candidateParams,
            JType candidateReturn, JType samReturn, boolean returnsVoid, List<JType> samThrows,
            JTypeConstraints.ExpressionCompatible constraint) {
        Map<Constraint, Constraint.Status> constraints = new HashMap<>();
        constraints.put(Constraint.identity(Set.of(candidate, true)), Constraint.Status.TRUE);
        typeParamConstraints.forEach(c -> constraints.put(c, Constraint.Status.UNKNOWN));

        int fixed = candidateParams.size() - 1;
        JArrayType vararg = (JArrayType) candidateParams.get(fixed);
        JType component = vararg.component();

        for (int i = 0; i < fixed; i++) {
            constraints.put(
                    new JTypeConstraints.Compatible(samParams.get(i), constraint.middle(), candidateParams.get(i)),
                    Constraint.Status.UNKNOWN);
        }

        for (int i = fixed; i < samParams.size(); i++) {
            constraints.put(
                    new JTypeConstraints.Compatible(samParams.get(i), constraint.middle(), component),
                    Constraint.Status.UNKNOWN);
        }

        if (!returnsVoid) {
            constraints.put(
                    new JTypeConstraints.Compatible(candidateReturn, constraint.middle(), samReturn),
                    Constraint.Status.UNKNOWN);
        }

        if (!JMemberLookup.addThrowsConstraints(constraints, candidate, samThrows)) return null;

        return new ConstraintBranch.Snapshot(new PropertySet(), constraints);
    }

    /**
     * Branch for unbound instance method references (JLS §18.2.1 n=k+1 case):
     * First SAM param is the receiver, remaining params match candidate params.
     */
    private static ConstraintBranch.Snapshot createUnboundRefBranch(
            JMethodReference candidate, Set<Constraint> typeParamConstraints,
            JClassReference classRef,
            List<JType> samParams, List<JType> candidateParams,
            JType candidateReturn, JType samReturn, boolean returnsVoid, List<JType> samThrows,
            JTypeConstraints.ExpressionCompatible constraint) {
        Map<Constraint, Constraint.Status> constraints = new HashMap<>();
        constraints.put(Constraint.identity(Set.of(candidate, false)), Constraint.Status.TRUE);
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

        if (!JMemberLookup.addThrowsConstraints(constraints, candidate, samThrows)) return null;

        return new ConstraintBranch.Snapshot(new PropertySet(), constraints);
    }

    /**
     * Branch for variable arity unbound instance method references (JLS §15.13.2):
     * first SAM param is the receiver, following fixed SAM params match the candidate's
     * fixed params directly, and the remaining trailing SAM params are spread into the
     * candidate's vararg array component type.
     */
    private static ConstraintBranch.Snapshot createUnboundVarargRefBranch(
            JMethodReference candidate, Set<Constraint> typeParamConstraints,
            JClassReference classRef,
            List<JType> samParams, List<JType> candidateParams,
            JType candidateReturn, JType samReturn, boolean returnsVoid, List<JType> samThrows,
            JTypeConstraints.ExpressionCompatible constraint) {
        Map<Constraint, Constraint.Status> constraints = new HashMap<>();
        constraints.put(Constraint.identity(Set.of(candidate, true)), Constraint.Status.TRUE);
        typeParamConstraints.forEach(c -> constraints.put(c, Constraint.Status.UNKNOWN));

        // First SAM param <: ReferenceType (receiver type check)
        constraints.put(new JTypeConstraints.Subtype(samParams.get(0), classRef), Constraint.Status.UNKNOWN);

        int fixed = candidateParams.size() - 1;
        JArrayType vararg = (JArrayType) candidateParams.get(fixed);
        JType component = vararg.component();

        for (int i = 0; i < fixed; i++) {
            constraints.put(
                    new JTypeConstraints.Compatible(samParams.get(i + 1), constraint.middle(), candidateParams.get(i)),
                    Constraint.Status.UNKNOWN);
        }

        for (int i = fixed + 1; i < samParams.size(); i++) {
            constraints.put(
                    new JTypeConstraints.Compatible(samParams.get(i), constraint.middle(), component),
                    Constraint.Status.UNKNOWN);
        }

        if (!returnsVoid) {
            constraints.put(
                    new JTypeConstraints.Compatible(candidateReturn, constraint.middle(), samReturn),
                    Constraint.Status.UNKNOWN);
        }

        if (!JMemberLookup.addThrowsConstraints(constraints, candidate, samThrows)) return null;

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
