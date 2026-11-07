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
import honeyroasted.jype.system.solver.JReferenceSupport;
import honeyroasted.jype.system.solver.JStandaloneType;
import honeyroasted.jype.system.solver.JStandaloneTyping;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.inference.JResolveMostSpecificMethod;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JReduceInvocationRefExpression extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown() && constraint.left() instanceof JExpressionInformation.InvocationReference<?>;
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.InvocationReference<?> ref = (JExpressionInformation.InvocationReference<?>) constraint.left();
        JType target = constraint.right();
        JTypeSystem system = target.typeSystem();

        if (target instanceof JMetaVarType mvt) {
            // JLS §18.5.2.2 the target is an input variable
            JReferenceSupport.defer(branch, constraint, Set.of(mvt));
            return;
        }
        Optional<JReferenceSupport.FunctionType> fn = JReferenceSupport.functionType(target);
        if (fn.isEmpty()) {
            // JLS §15.13.2 the target must be a functional interface type
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }
        List<JType> params = fn.get().parameters();
        if (!JResolveMostSpecificMethod.isExactMethodReference(system, ref)) {
            Set<JMetaVarType> missing = new LinkedHashSet<>();
            Optional<List<JType>> resolved = JReferenceSupport.resolveInputs(branch, params, missing);
            if (resolved.isEmpty()) {
                // JLS §18.5.2.2 an inexact method reference waits for its parameter types
                JReferenceSupport.defer(branch, constraint, missing);
                return;
            }
            params = resolved.get();
        }
        JType ret = fn.get().returnType();
        List<JType> samThrows = fn.get().sam().exceptionTypes();
        List<JArgumentType> explicit = ref.explicitTypeArguments() == null ? List.of() : ref.explicitTypeArguments();

        List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
        if (ref.source() instanceof JExpressionInformation.SuperReference superRef) {
            JClassType site = superSite(superRef, ref.declaring());
            if (site != null) {
                Search s = search(system, ref, site, site, params, explicit, ret, samThrows, false, Map.of());
                if (s.selection().chosen() != null && !s.selection().chosen().hasModifier(AccessFlag.STATIC) && !s.selection().chosen().hasModifier(AccessFlag.ABSTRACT)) {
                    // JLS §15.13.1 super::m must name a non-static, non-abstract method
                    s.selection().snapshot().ifPresent(newChildren::add);
                }
            }
        } else if (ref.source() instanceof JExpressionInformation expr) {
            for (JStandaloneType receiver : JStandaloneTyping.resolve(expr, system)) {
                if (receiver.type() instanceof JPrimitiveType) {
                    continue;
                }
                Map<Constraint, Constraint.Status> extra = new LinkedHashMap<>();
                if (receiver.extra() != null) {
                    extra.put(receiver.extra(), Constraint.Status.UNKNOWN);
                }
                Search s = search(system, ref, receiver.type(), receiver.type() instanceof JClassType ct ? ct : null, params, explicit, ret, samThrows, false, extra);
                if (s.selection().chosen() != null && !s.selection().chosen().hasModifier(AccessFlag.STATIC)) {
                    // JLS §15.13.1 Primary::m must name an instance method
                    s.selection().snapshot().ifPresent(snap -> newChildren.add(receiver.branch() == null ? snap : JStandaloneTyping.combine(receiver.branch(), snap)));
                }
            }
        } else if (ref.source() instanceof JClassType refType) {
            // JLS §15.13.1 ReferenceType::m performs two searches
            Search first = search(system, ref, refType, null, params, explicit, ret, samThrows, true, Map.of());
            Search second = null;
            if (!params.isEmpty()) {
                JType p1 = params.getFirst();
                JClassType searchType = refType;
                if (JTypeRelations.isRaw(refType)) {
                    JClassType sup = JTypeRelations.asSuper(p1, refType.classReference());
                    if (sup != null && !JTypeRelations.isRaw(sup)) {
                        searchType = JTypeRelations.capture(sup);
                    }
                }
                Map<Constraint, Constraint.Status> extra = new LinkedHashMap<>();
                // JLS §18.2.1 ‹P1 <: ReferenceType› for the unbound receiver
                extra.put(new JTypeConstraints.Subtype(p1, searchType), Constraint.Status.UNKNOWN);
                second = search(system, ref, searchType, searchType, params.subList(1, params.size()), explicit, ret, samThrows, false, extra);
            }
            JMethodReference c1 = first.selection().chosen();
            JMethodReference c2 = second == null ? null : second.selection().chosen();
            boolean secondHasInstance = second != null && second.selection().applicable().stream().anyMatch(m -> !m.hasModifier(AccessFlag.STATIC));
            boolean firstHasStatic = first.selection().applicable().stream().anyMatch(m -> m.hasModifier(AccessFlag.STATIC));
            if (c1 != null && c1.hasModifier(AccessFlag.STATIC) && !secondHasInstance) {
                first.selection().snapshot().ifPresent(newChildren::add);
            } else if (!firstHasStatic && c2 != null && !c2.hasModifier(AccessFlag.STATIC)) {
                second.selection().snapshot().ifPresent(newChildren::add);
            }
        }

        if (!newChildren.isEmpty()) {
            branch.drop(constraint).divergeBranches(newChildren);
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }

    private record Search(JInvocationSelection.Selection selection) {
    }

    private static Search search(JTypeSystem system, JExpressionInformation.InvocationReference<?> ref, JType site, JClassType receiverType, List<JType> argTypes, List<JArgumentType> explicit,
                                 JType ret, List<JType> samThrows, boolean staticForm, Map<Constraint, Constraint.Status> extra) {
        JClassReference declaring = ref.declaring();
        List<JInvocationCandidate> candidates = new ArrayList<>();
        for (JMethodLookup.Member member : JMethodLookup.members(site, ref.methodName())) {
            JMethodReference m = member.method();
            if (declaring != null && !m.outerClass().accessFrom(declaring, staticForm || m.hasModifier(AccessFlag.STATIC) ? null : receiverType).canAccess(m.access())) {
                continue;
            }
            JType result = m.returnType();
            if (m.location().name().equals("getClass") && m.parameters().isEmpty() && member.declaringType().classReference().typeEquals(system.constants().object())) {
                result = JMethodLookup.getClassReturnType(site);
            }
            candidates.add(new JInvocationCandidate(m, m.parameters(), m.typeParameters(), m.typeParameters(), result, m.exceptionTypes(), m.hasModifier(AccessFlag.VARARGS)));
        }
        List<JExpressionInformation> args = argTypes.stream().map(p -> (JExpressionInformation) JExpressionInformation.of(p)).toList();
        JExpressionInformation.MethodInvocation<?> synthetic = new JExpressionInformation.MethodInvocation.Simple<>(declaring, null, site instanceof JClassType ct ? ct.classReference() : JExpressionInformation.of(site), ref.methodName(), args, explicit);
        Map<Constraint, Constraint.Status> all = new LinkedHashMap<>(extra);
        all.put(new JTypeConstraints.LambdaThrowsCheck(synthetic, samThrows), Constraint.Status.UNKNOWN);
        // JLS §18.2.1 ‹R' → R› unless R is void
        JTypeConstraints.ExpressionCompatible returnConstraint = new JTypeConstraints.ExpressionCompatible(synthetic, JTypeConstraints.Compatible.Context.ASSIGNMENT, ret);
        return new Search(JInvocationSelection.selectDetailed(system, synthetic, candidates, args, explicit, returnConstraint, all));
    }

    private static JClassType superSite(JExpressionInformation.SuperReference superRef, JClassReference declaring) {
        if (declaring == null) {
            return superRef.outer().superClass();
        }
        if (superRef.outer().hasModifier(AccessFlag.INTERFACE)) {
            for (JClassType iface : declaring.interfaces()) {
                if (iface.classReference().typeEquals(superRef.outer())) {
                    return iface;
                }
            }
            return null;
        }
        JClassType current = declaring;
        while (current != null) {
            if (current.classReference().equals(superRef.outer())) {
                return current.superClass();
            }
            current = current.outerType();
        }
        return null;
    }
}
