package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JMemberLookup;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.visitor.JTypeVisitors;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JReduceLambdaExpression extends ConstraintMapper.Unary<JTypeConstraints.ExpressionCompatible> {

    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        return status.isUnknown()
                && constraint.left() instanceof JExpressionInformation.Lambda
                && (constraint.right() instanceof JClassType || constraint.right() instanceof JIntersectionType);
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint, Constraint.Status status) {
        JExpressionInformation.Lambda lambda = (JExpressionInformation.Lambda) constraint.left();
        JType targetType = constraint.right();

        // Find the Single Abstract Method - JLS §9.8 (may be an intersection of interfaces)
        Optional<JMethodReference> samOpt = findFunctionalMethod(targetType);
        if (samOpt.isEmpty()) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        JMethodReference sam = samOpt.get();
        List<JType> samParams = sam.parameters();
        JType samReturn = sam.returnType();
        boolean returnsVoid = samReturn.typeEquals(targetType.typeSystem().constants().voidType());

        // JLS §18.2.1: lambda parameter count must match SAM parameter count
        if (lambda.parameterCount() != samParams.size()) {
            branch.set(constraint, Constraint.Status.FALSE);
            return;
        }

        // JLS §18.5.2: an implicitly typed lambda is not "pertinent to applicability" - since it
        // has no declared parameter types, it contributes no parameter constraints regardless of
        // whether the SAM's parameter types are themselves still-uninferred inference variables.
        // Only its body (checked below via the return-type constraint) is ever compared against
        // the target, so param-properness is not required here.
        boolean explicitlyTyped = lambda.explicitParameterTypes() != null
                && lambda.explicitParameterTypes().size() == lambda.parameterCount();

        // Collect sub-constraints before deciding how to reduce
        List<Constraint> subConstraints = new ArrayList<>();

        // JLS §18.2.1: explicit parameter type equality constraints ‹Fi = Gi›
        if (explicitlyTyped) {
            for (int i = 0; i < lambda.parameterCount(); i++) {
                subConstraints.add(new JTypeConstraints.Equal(lambda.explicitParameterTypes().get(i), samParams.get(i)));
            }
        }

        // JLS §18.2.6/§8.4.6: a lambda body that is itself a method/constructor invocation must
        // not throw a checked exception the SAM's throws clause doesn't cover. Which method gets
        // chosen isn't known until the body's own invocation is resolved, so - unlike the deferred
        // ‹body → R› constraint below - this resolves the body eagerly (mirroring how
        // JReduceBinaryOperator/JReduceConditionalExpression resolve a non-simply-typed operand via
        // their own sub-solve) and discards any candidate whose chosen method throws something
        // uncovered. A lambda body that isn't itself an invocation (e.g. a constant, a nested
        // conditional, another lambda) can't directly throw anything when evaluated, so this only
        // applies to that one case; see the class-level notes for why the general "what can this
        // body throw" question isn't answered for arbitrary bodies.
        if (lambda.body() instanceof JExpressionInformation.Invocation) {
            resolveInvocationBodyWithThrowsCheck(branch, constraint, lambda, sam, samReturn, returnsVoid, subConstraints);
            return;
        }

        // JLS §18.2.1: return type constraint ‹body → R›
        if (!returnsVoid) {
            subConstraints.add(new JTypeConstraints.ExpressionCompatible(
                    lambda.body(),
                    JTypeConstraints.Compatible.Context.ASSIGNMENT,
                    samReturn));
        }

        // If nothing to prove, the constraint is trivially satisfied
        if (subConstraints.isEmpty()) {
            branch.set(constraint, Constraint.Status.TRUE);
        } else {
            branch.drop(constraint);
            subConstraints.forEach(branch::add);
        }
    }

    private static void resolveInvocationBodyWithThrowsCheck(ConstraintBranch branch, JTypeConstraints.ExpressionCompatible constraint,
                                                               JExpressionInformation.Lambda lambda, JMethodReference sam, JType samReturn,
                                                               boolean returnsVoid, List<Constraint> explicitParamConstraints) {
        JExpressionInformation body = lambda.body();
        JTypeSystem system = samReturn.typeSystem();

        // A void SAM never imposed a return-type constraint on the body to begin with (the body's
        // own value, if any, is simply discarded) - so resolve it standalone (an unconstrained
        // metavariable target) purely to discover which overload gets chosen for the throws check.
        JMetaVarType openTarget = returnsVoid ? system.typeFactory().newMetaVarType("LAMBDA_BODY") : null;
        JType bodyTarget = returnsVoid ? openTarget : samReturn;
        JTypeConstraints.Compatible.Context bodyContext = returnsVoid ?
                JTypeConstraints.Compatible.Context.STRICT_INVOCATION : JTypeConstraints.Compatible.Context.ASSIGNMENT;

        ConstraintTree solved = system.operations().inferenceSolver()
                .bind(new JTypeConstraints.ExpressionCompatible(body, bodyContext, bodyTarget))
                .solve();

        List<ConstraintBranch.Snapshot> newChildren = new ArrayList<>();
        solved.branches().forEach(childBranch -> {
            if (!childBranch.status().isTrue()) return;

            Optional<JMethodReference> chosen = childBranch.metadata().all(JTypeContext.ChosenMethod.class).stream()
                    .filter(cm -> cm.expression() == body)
                    .map(JTypeContext.ChosenMethod::chosen)
                    .findFirst();

            Map<Constraint, Constraint.Status> extra = new LinkedHashMap<>();
            boolean throwsOk = chosen.map(ref -> JMemberLookup.addThrowsConstraints(extra, ref, sam.exceptionTypes())).orElse(true);
            if (!throwsOk) return;

            explicitParamConstraints.forEach(c -> extra.putIfAbsent(c, Constraint.Status.UNKNOWN));
            if (openTarget != null) {
                childBranch.metadata().first(JTypeContext.TypeMetavarMap.class).ifPresent(mvp -> mvp.instantiations().remove(openTarget));
            }

            newChildren.add(combine(childBranch, new ConstraintBranch.Snapshot(new PropertySet(), extra)));
        });

        if (!newChildren.isEmpty()) {
            branch.drop(constraint).divergeBranches(newChildren);
        } else {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }

    private static ConstraintBranch.Snapshot combine(ConstraintBranch a, ConstraintBranch.Snapshot b) {
        a.constraints().forEach(b.constraints()::putIfAbsent);
        a.metadata().remove(JTypeContext.TypeMetavarMap.class);
        b.metadata().inheritFrom(a.metadata());
        return b;
    }

    /**
     * Finds the Single Abstract Method (SAM) of the given functional interface type per JLS §9.8,
     * resolving its type variables through whichever intersection member (or the type itself)
     * declares/inherits it. The target may be a plain interface type or an intersection of
     * interfaces (JLS §9.8, e.g. a cast target {@code (Runnable & Serializable)}); at most one
     * non-interface member is tolerated, and only if it is {@code Object}. Returns empty if the
     * type is not a functional interface, or if the intersection doesn't reduce to exactly one
     * abstract method overall.
     */
    public static Optional<JMethodReference> findFunctionalMethod(JType targetType) {
        List<JClassType> roots = new ArrayList<>();
        if (targetType instanceof JIntersectionType it) {
            for (JType child : it.children()) {
                if (!(child instanceof JClassType ct)) return Optional.empty();
                roots.add(ct);
            }
        } else if (targetType instanceof JClassType ct) {
            roots.add(ct);
        } else {
            return Optional.empty();
        }

        if (roots.stream().noneMatch(r -> r.classReference().hasModifier(AccessFlag.INTERFACE))) {
            return Optional.empty();
        }

        Set<JClassReference> visited = new LinkedHashSet<>();
        Map<JMethodReference, JClassType> owningRoot = new LinkedHashMap<>();
        List<JMethodReference> allMethods = new ArrayList<>();

        for (JClassType root : roots) {
            JClassReference ref = root.classReference();
            if (!ref.hasModifier(AccessFlag.INTERFACE)) {
                if (!ref.typeEquals(root.typeSystem().constants().object())) return Optional.empty();
                continue;
            }

            for (JMethodReference m : collectInterfaceMethods(ref, visited)) {
                if (allMethods.stream().noneMatch(e -> isOverrideEquivalent(m, e))) {
                    allMethods.add(m);
                    owningRoot.put(m, root);
                }
            }
        }

        List<JMethodReference> abstractMethods = new ArrayList<>();
        for (JMethodReference m : allMethods) {
            if (m.hasModifier(AccessFlag.ABSTRACT)
                    && !m.hasModifier(AccessFlag.STATIC)
                    && !isObjectPublicMethod(m)) {
                abstractMethods.add(m);
            }
        }

        if (abstractMethods.size() != 1) return Optional.empty();

        JMethodReference sam = abstractMethods.getFirst();
        JClassType owner = owningRoot.get(sam);
        if (owner instanceof JParameterizedClassType pct) {
            sam = (JMethodReference) pct.varTypeResolver().apply(sam);
        }

        return Optional.of(sam);
    }

    /**
     * Collects all non-static non-private methods from the interface hierarchy,
     * deduplicating by override-equivalence so the most specific declaration wins.
     */
    private static List<JMethodReference> collectInterfaceMethods(JClassReference ref, Set<JClassReference> visited) {
        if (!visited.add(ref)) return Collections.emptyList();

        List<JMethodReference> result = new ArrayList<>();

        for (JMethodReference m : ref.declaredMethods()) {
            if (!m.hasModifier(AccessFlag.STATIC) && !m.hasModifier(AccessFlag.PRIVATE)) {
                if (result.stream().noneMatch(e -> isOverrideEquivalent(m, e))) {
                    result.add(m);
                }
            }
        }

        for (JClassType iface : ref.interfaces()) {
            for (JMethodReference m : collectInterfaceMethods(iface.classReference(), visited)) {
                if (result.stream().noneMatch(e -> isOverrideEquivalent(m, e))) {
                    result.add(m);
                }
            }
        }

        return result;
    }

    /**
     * Two methods are override-equivalent when they share the same name and erased parameter types (JLS §8.4.2).
     */
    static boolean isOverrideEquivalent(JMethodReference a, JMethodReference b) {
        if (!a.location().name().equals(b.location().name())) return false;
        if (a.parameters().size() != b.parameters().size()) return false;
        JMethodReference ea = (JMethodReference) JTypeVisitors.ERASURE.visit(a);
        JMethodReference eb = (JMethodReference) JTypeVisitors.ERASURE.visit(b);
        for (int i = 0; i < ea.parameters().size(); i++) {
            if (!ea.parameters().get(i).typeEquals(eb.parameters().get(i))) return false;
        }
        return true;
    }

    /**
     * Returns true when this method's signature matches a public instance method of Object.
     * Such methods are excluded from the SAM count per JLS §9.8.
     */
    private static boolean isObjectPublicMethod(JMethodReference m) {
        String name = m.location().name();
        int p = m.parameters().size();
        return switch (name) {
            case "equals" -> p == 1;
            case "hashCode", "toString", "getClass", "notify", "notifyAll" -> p == 0;
            case "wait" -> p == 0 || p == 1 || p == 2;
            default -> false;
        };
    }
}
