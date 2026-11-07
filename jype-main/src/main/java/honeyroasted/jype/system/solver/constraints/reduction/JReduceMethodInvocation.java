package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JInvocationCandidate;
import honeyroasted.jype.system.solver.JInvocationSelection;
import honeyroasted.jype.system.solver.JMethodLookup;
import honeyroasted.jype.system.solver.JStandaloneType;
import honeyroasted.jype.system.solver.JStandaloneTyping;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JReduceMethodInvocation extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.MethodInvocation<?> invoc &&
                (invoc.source() instanceof JClassReference || invoc.source() instanceof JExpressionInformation ||
                        invoc.source() instanceof JExpressionInformation.SuperReference || invoc.source() instanceof JExpressionInformation.ImplicitReceiver);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.MethodInvocation<?> invocation = (JExpressionInformation.MethodInvocation<?>) constraint.left();
        JTypeSystem system = constraint.right().typeSystem();
        JClassReference declaring = invocation.declaring();

        List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
        if (invocation.source() instanceof JClassReference ref) {
            // JLS §15.12.1 TypeName.m: the class to search is TypeName
            select(system, invocation, ref, null, true, false, constraint, Map.of()).ifPresent(newChildren::add);
        } else if (invocation.source() instanceof JExpressionInformation.ImplicitReceiver) {
            // JLS §15.12.1 an unqualified name searches the innermost enclosing class with a member of that name
            JClassType current = declaring;
            boolean staticContext = invocation.declaringMethod() != null && invocation.declaringMethod().hasModifier(AccessFlag.STATIC);
            while (current != null) {
                JClassType site = current.classReference().typeParameters().isEmpty() ? current.classReference() : current.classReference().parameterizedWithTypeVars();
                if (!JMethodLookup.members(site, invocation.name()).isEmpty()) {
                    Optional<ConstraintBranch.Snapshot> snapshot = select(system, invocation, site, site, false, false, constraint, Map.of());
                    boolean finalStatic = staticContext;
                    snapshot.filter(s -> !finalStatic || s.metadata().first(JTypeContext.ChosenMethod.class).map(cm -> cm.chosen().hasModifier(AccessFlag.STATIC)).orElse(true))
                            .ifPresent(newChildren::add);
                    break;
                }
                if (current.classReference().hasModifier(AccessFlag.STATIC)) {
                    staticContext = true;
                }
                current = current.hasOuterType() ? current.outerType() : null;
            }
        } else if (invocation.source() instanceof JExpressionInformation.SuperReference superRef) {
            JClassType site = superSite(superRef, declaring);
            if (site != null) {
                select(system, invocation, site, enclosingInstance(superRef.outer(), declaring), false, true, constraint, Map.of()).ifPresent(newChildren::add);
            }
        } else if (invocation.source() instanceof JExpressionInformation expr) {
            for (JStandaloneType receiver : JStandaloneTyping.resolve(expr, system)) {
                if (receiver.type() instanceof JPrimitiveType || receiver.type() instanceof JNoneType) {
                    // JLS §15.12.1 a primitive or null receiver cannot be dereferenced
                    continue;
                }
                Map<Constraint, Constraint.Status> extra = new LinkedHashMap<>();
                if (receiver.extra() != null) {
                    extra.put(receiver.extra(), Constraint.Status.UNKNOWN);
                }
                Optional<ConstraintBranch.Snapshot> snapshot = select(system, invocation, receiver.type(), receiver.type() instanceof JClassType ct ? ct : null, false, false, constraint, extra);
                snapshot.ifPresent(s -> newChildren.add(receiver.branch() == null ? s : JStandaloneTyping.combine(receiver.branch(), s)));
            }
        }

        if (!newChildren.isEmpty()) {
            branch.drop(constraint).divergeBranches(newChildren);
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }

    private static Optional<ConstraintBranch.Snapshot> select(JTypeSystem system, JExpressionInformation.MethodInvocation<?> invocation, JType site, JClassType receiverType, boolean staticForm, boolean superForm,
                                                             JTypeConstraints.ExpressionCompatible constraint, Map<Constraint, Constraint.Status> extra) {
        JClassReference declaring = invocation.declaring();
        List<JInvocationCandidate> candidates = new ArrayList<>();
        for (JMethodLookup.Member member : JMethodLookup.members(site, invocation.name())) {
            JMethodReference m = member.method();
            if (declaring != null && !m.outerClass().accessFrom(declaring, staticForm || superForm || m.hasModifier(AccessFlag.STATIC) ? null : receiverType).canAccess(m.access())) {
                // JLS §15.12.2.1 the member must be accessible
                continue;
            }
            JType result = m.returnType();
            if (!staticForm && m.location().name().equals("getClass") && m.parameters().isEmpty() && member.declaringType().classReference().typeEquals(system.constants().object())) {
                result = JMethodLookup.getClassReturnType(site);
            }
            candidates.add(new JInvocationCandidate(m, m.parameters(), m.typeParameters(), m.typeParameters(), result, m.exceptionTypes(), m.hasModifier(AccessFlag.VARARGS)));
        }
        Optional<ConstraintBranch.Snapshot> snapshot = JInvocationSelection.select(system, invocation, candidates, invocation.parameters(), invocation.explicitTypeArguments(), constraint, extra);
        if (snapshot.isPresent()) {
            JMethodReference chosen = snapshot.get().metadata().first(JTypeContext.ChosenMethod.class).map(JTypeContext.ChosenMethod::chosen).orElse(null);
            if (chosen != null && staticForm && !chosen.hasModifier(AccessFlag.STATIC)) {
                // JLS §15.12.3 TypeName.m must be a static method
                return Optional.empty();
            }
            if (chosen != null && superForm && chosen.hasModifier(AccessFlag.ABSTRACT)) {
                // JLS §15.12.3 super.m must not be abstract
                return Optional.empty();
            }
        }
        return snapshot;
    }

    private static JClassType superSite(JExpressionInformation.SuperReference superRef, JClassReference declaring) {
        if (declaring == null) {
            return null;
        }
        if (superRef.outer().hasModifier(AccessFlag.INTERFACE)) {
            for (JClassType iface : declaring.interfaces()) {
                if (iface.classReference().typeEquals(superRef.outer())) {
                    // JLS §15.12.1 TypeName.super.m with TypeName a direct superinterface
                    return iface;
                }
            }
            return null;
        }
        JClassType outer = enclosingInstance(superRef.outer(), declaring);
        // JLS §15.12.1 super.m and TypeName.super.m search the superclass
        return outer == null ? null : outer.superClass();
    }

    private static JClassType enclosingInstance(JClassReference qualifier, JClassReference declaring) {
        JClassType current = declaring;
        while (current != null) {
            if (current.classReference().equals(qualifier)) {
                return current;
            }
            current = current.outerType();
        }
        return null;
    }
}
