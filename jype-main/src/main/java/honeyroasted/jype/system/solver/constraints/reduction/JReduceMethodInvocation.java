package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.collect.multi.Pair;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.JMemberLookup;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.solver.constraints.inference.JResolveBounds;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JReduceMethodInvocation extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.MethodInvocation<?> invoc &&
                (invoc.source() instanceof JClassReference || invoc.source() instanceof JExpressionInformation ||
                        invoc.source() instanceof JExpressionInformation.SuperReference);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.MethodInvocation<?> invocation = (JExpressionInformation.MethodInvocation<?>) constraint.left();
        JType targetType = constraint.right();
        JTypeSystem system = targetType.typeSystem();

        JClassReference declaring = invocation.declaring();
        List<JExpressionInformation> parameters = invocation.parameters();

        Collection<JMethodReference> methods = new LinkedHashSet<>();
        boolean stat;
        JClassType receiverType = null;

        if (invocation.source() instanceof JClassReference ref) { //Static method call
            methods.addAll(JMemberLookup.staticMethods(ref));
            stat = true;
        } else if (invocation.source() instanceof JExpressionInformation.SuperReference superRef) { //(possibly qualified) super invocation
            JClassType outerInstance = findEnclosingInstance(superRef.outer(), declaring);
            JClassType superClass = outerInstance == null ? null : outerInstance.superClass();

            if (superClass == null) {
                branch.set(constraint, Constraint.Status.FALSE);
                return;
            }

            // Search begins at the superclass itself (JLS §15.12.1), so overrides declared in
            // outerInstance are skipped, matching super's non-virtual dispatch.
            JMemberLookup.allMethods(superClass.classReference()).forEach(ref -> {
                JMethodReference resolved = ref;
                if (superClass instanceof JParameterizedClassType pct) {
                    resolved = (JMethodReference) pct.varTypeResolver().visit(resolved);
                }
                methods.add(resolved);
            });

            receiverType = outerInstance;
            stat = false;
        } else if (invocation.source() instanceof JExpressionInformation expr) { //Instance method call
            if (expr.isSimplyTyped()) {
                JType type = expr.getSimpleType(system, branchContext.firstOr(JTypeContext.TypeMetavarMap.class, JTypeContext.TypeMetavarMap.empty())).get();
                type = JMemberLookup.captureConvert(branch, type); // JLS §5.1.10
                receiverType = type instanceof JClassType ct ? ct : null;
                JMemberLookup.findClassTypes(type).forEach(ct -> JMemberLookup.allMethods(ct.classReference()).forEach(ref -> {
                    if (ct instanceof JParameterizedClassType pct) {
                        ref = (JMethodReference) pct.varTypeResolver().visit(ref);
                    }
                    methods.add(ref);
                }));

                stat = false;
            } else {
                JMetaVarType callTarget = system.typeFactory().newMetaVarType("CALL_TARGET");
                ConstraintTree solved = system.operations().inferenceSolver()
                        .bind(new JTypeConstraints.ExpressionCompatible(expr, JTypeConstraints.Compatible.Context.STRICT_INVOCATION, callTarget))
                        .solve();

                List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
                solved.branches().forEach(childBranch -> {
                    if (childBranch.status().isTrue()) {
                        JType inst = JResolveBounds.findInstantiation(callTarget, childBranch);
                        inst = JMemberLookup.captureConvert(childBranch, inst); // JLS §5.1.10
                        JClassType instReceiverType = inst instanceof JClassType ct ? ct : null;
                        childBranch.metadata().first(JTypeContext.TypeMetavarMap.class).ifPresent(mvp -> mvp.instantiations().remove(callTarget)); //Drop variable since it is no longer needed
                        JMemberLookup.findClassTypes(inst).forEach(ct -> JMemberLookup.allMethods(ct.classReference()).forEach(ref -> {
                            if (ct instanceof JParameterizedClassType pct) {
                                ref = (JMethodReference) pct.varTypeResolver().visit(ref);
                            }

                            if (ref.location().name().equals(invocation.name()) && ref.outerClass().accessFrom(declaring, instReceiverType).canAccess(ref.access())) {
                                if (ref.parameters().size() == parameters.size()) {
                                    newChildren.add(combine(childBranch, createInvoke(JTypeConstraints.Compatible.Context.STRICT_INVOCATION, constraint, invocation, ref, parameters)));
                                    newChildren.add(combine(childBranch, createInvoke(JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, constraint, invocation, ref, parameters)));
                                }

                                if (ref.hasModifier(AccessFlag.VARARGS) && parameters.size() >= ref.parameters().size() - 1 &&
                                        ref.parameters().get(ref.parameters().size() - 1) instanceof JArrayType vararg) {
                                    newChildren.add(combine(childBranch, createVarargInvoke(JTypeConstraints.Compatible.Context.STRICT_INVOCATION, constraint, invocation, ref, parameters, vararg)));
                                    newChildren.add(combine(childBranch, createVarargInvoke(JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, constraint, invocation, ref, parameters, vararg)));
                                }
                            }
                        }));
                    } else {
                        newChildren.add(childBranch.snapshot());
                    }
                });

                if (!newChildren.isEmpty()) {
                    branch.drop(constraint).divergeBranches(newChildren);
                } else {
                    branch.set(constraint, Constraint.Status.FALSE);
                }
                return;
            }
        } else {
            stat = false;
        }

        if (methods.isEmpty()) {
            branch.set(constraint, Constraint.Status.FALSE);
        } else {
            List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
            JClassType finalReceiverType = receiverType;

            methods.forEach(ref -> {
                if (ref.location().name().equals(invocation.name()) && ref.outerClass().accessFrom(declaring, finalReceiverType).canAccess(ref.access()) && (!stat || ref.hasModifier(AccessFlag.STATIC))) {
                    if (ref.parameters().size() == parameters.size()) {
                        newChildren.add(createInvoke(JTypeConstraints.Compatible.Context.STRICT_INVOCATION, constraint, invocation, ref, parameters));
                        newChildren.add(createInvoke(JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, constraint, invocation, ref, parameters));
                    }

                    if (ref.hasModifier(AccessFlag.VARARGS) && parameters.size() >= ref.parameters().size() - 1 &&
                            ref.parameters().get(ref.parameters().size() - 1) instanceof JArrayType vararg) {
                        newChildren.add(createVarargInvoke(JTypeConstraints.Compatible.Context.STRICT_INVOCATION, constraint, invocation, ref, parameters, vararg));
                        newChildren.add(createVarargInvoke(JTypeConstraints.Compatible.Context.LOOSE_INVOCATION, constraint, invocation, ref, parameters, vararg));
                    }
                }
            });

            if (!newChildren.isEmpty()) {
                branch.drop(constraint).divergeBranches(newChildren);
            } else {
                branch.set(constraint, Constraint.Status.FALSE);
            }
        }
    }

    /**
     * Walks the declaring class's own type and lexically enclosing chain looking for a class
     * exactly matching {@code qualifier}, per JLS §15.12.1's requirement that {@code Outer} in
     * {@code Outer.super.method()} name a lexically enclosing class of the invocation site (or,
     * for a plain unqualified {@code super}, the invocation site's own class).
     */
    private static JClassType findEnclosingInstance(JClassReference qualifier, JClassReference declaring) {
        JClassType current = declaring;
        while (current != null) {
            if (current.classReference().equals(qualifier)) {
                return current;
            }
            current = current.outerType();
        }
        return null;
    }

    private static ConstraintBranch.Snapshot combine(ConstraintBranch a, ConstraintBranch.Snapshot b) {
        a.constraints().forEach(b.constraints()::putIfAbsent);
        a.metadata().remove(JTypeContext.TypeMetavarMap.class);
        b.metadata().inheritFrom(a.metadata());
        return b;
    }

    private static ConstraintBranch.Snapshot createInvoke(JTypeConstraints.Compatible.Context context, JTypeConstraints.ExpressionCompatible constraint, JExpressionInformation.MethodInvocation<?> invocation, JMethodReference ref, List<JExpressionInformation> parameters) {
        Map<Constraint, Constraint.Status> invoke = new HashMap<>();
        Pair<Set<Constraint>, JVarTypeResolveVisitor> typeParams = createTypeParams(ref, invocation);
        JVarTypeResolveVisitor resolve = typeParams.right();

        invoke.put(Constraint.identity(Set.of(ref, context, false)), Constraint.Status.TRUE);
        typeParams.left().forEach(c -> invoke.put(c, Constraint.Status.UNKNOWN));
        invoke.put(new JTypeConstraints.Compatible(resolve.apply(resolve.apply(ref.returnType())), constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);
        for (int i = 0; i < parameters.size(); i++) {
            invoke.put(new JTypeConstraints.ExpressionCompatible(parameters.get(i), context, resolve.apply(ref.parameters().get(i))), Constraint.Status.UNKNOWN);
        }
        return new ConstraintBranch.Snapshot(new PropertySet().attach(new JTypeContext.ChosenMethod(invocation, ref, context, false)), invoke);
    }

    private static ConstraintBranch.Snapshot createVarargInvoke(JTypeConstraints.Compatible.Context context, JTypeConstraints.ExpressionCompatible constraint, JExpressionInformation.MethodInvocation<?> invocation, JMethodReference ref, List<JExpressionInformation> parameters, JArrayType vararg) {
        Map<Constraint, Constraint.Status> invoke = new HashMap<>();
        Pair<Set<Constraint>, JVarTypeResolveVisitor> typeParams = createTypeParams(ref, invocation);
        JVarTypeResolveVisitor resolve = typeParams.right();

        invoke.put(Constraint.identity(Set.of(ref, context, true)), Constraint.Status.TRUE);
        typeParams.left().forEach(c -> invoke.put(c, Constraint.Status.UNKNOWN));
        invoke.put(new JTypeConstraints.Compatible(resolve.apply(resolve.apply(ref.returnType())), constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);

        int index;
        for (index = 0; index < ref.parameters().size() - 1; index++) {
            invoke.put(new JTypeConstraints.ExpressionCompatible(parameters.get(index), context, resolve.apply(ref.parameters().get(index))), Constraint.Status.UNKNOWN);
        }

        for (; index < parameters.size(); index++) {
            invoke.put(new JTypeConstraints.ExpressionCompatible(parameters.get(index), context, resolve.apply(vararg.component())), Constraint.Status.UNKNOWN);
        }

        return new ConstraintBranch.Snapshot(new PropertySet().attach(new JTypeContext.ChosenMethod(invocation, ref, context, true)), invoke);
    }

    private static Pair<Set<Constraint>, JVarTypeResolveVisitor> createTypeParams(JMethodReference ref, JExpressionInformation.MethodInvocation<?> invoc) {
        Set<Constraint> newConstraints = new LinkedHashSet<>();
        Map<JVarType, JType> resolution = new LinkedHashMap<>();

        if (invoc.explicitTypeArguments().isEmpty() || invoc.explicitTypeArguments().size() != ref.typeParameters().size()) {
            for (int i = 0; i < ref.typeParameters().size(); i++) {
                JVarType vt = ref.typeParameters().get(i);
                JMetaVarType mvt = vt.createMetaVar();

                newConstraints.add(new JTypeConstraints.Infer(mvt, vt));
                resolution.put(vt, mvt);
            }
        } else {
            for (int i = 0; i < ref.typeParameters().size(); i++) {
                resolution.put(ref.typeParameters().get(i), invoc.explicitTypeArguments().get(i));
            }
        }


        return Pair.of(newConstraints, new JVarTypeResolveVisitor(resolution));
    }

}
