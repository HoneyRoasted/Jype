package honeyroasted.jype.system.solver;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.solver.constraints.inference.JResolveMostSpecificMethod;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceLambdaExpression;
import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JRecursiveTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArgumentType;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JInvocationSelection {
    private JInvocationSelection() {
    }

    private enum Phase {
        STRICT, LOOSE, VARARG
    }

    private record Applicable(JInvocationCandidate candidate, Phase phase, boolean unchecked, ConstraintBranch b2, Map<JVarType, JType> theta) {
    }

    private record Tested(boolean unchecked, ConstraintBranch b2, Map<JVarType, JType> theta) {
    }

    private record Instantiated(Map<JVarType, JType> theta, List<Constraint> constraints, List<JType> formals) {
    }

    public record Selection(Optional<ConstraintBranch.Snapshot> snapshot, List<JMethodReference> applicable, JMethodReference chosen) {
    }

    public static Optional<ConstraintBranch.Snapshot> select(JTypeSystem system, JExpressionInformation.Invocation expr, List<JInvocationCandidate> candidates, List<JExpressionInformation> args,
                                                            List<JArgumentType> explicitTypeArguments, JTypeConstraints.ExpressionCompatible constraint, Map<Constraint, Constraint.Status> extra) {
        return selectDetailed(system, expr, candidates, args, explicitTypeArguments, constraint, extra).snapshot();
    }

    public static Selection selectDetailed(JTypeSystem system, JExpressionInformation.Invocation expr, List<JInvocationCandidate> candidates, List<JExpressionInformation> args,
                                           List<JArgumentType> explicitTypeArguments, JTypeConstraints.ExpressionCompatible constraint, Map<Constraint, Constraint.Status> extra) {
        List<Applicable> applicable = new ArrayList<>();
        for (Phase phase : Phase.values()) {
            for (JInvocationCandidate candidate : candidates) {
                if (potentiallyApplicable(system, candidate, args, explicitTypeArguments, phase)) {
                    testApplicable(system, candidate, args, explicitTypeArguments, phase).ifPresent(t -> applicable.add(new Applicable(candidate, phase, t.unchecked(), t.b2(), t.theta())));
                }
            }
            if (!applicable.isEmpty()) {
                // JLS §15.12.2 the first phase with an applicable method determines the candidates
                break;
            }
        }
        List<JMethodReference> applicableMethods = applicable.stream().map(a -> a.candidate().method()).toList();
        if (applicable.isEmpty()) {
            return new Selection(Optional.empty(), applicableMethods, null);
        }

        Applicable chosen = mostSpecific(system, expr, applicable);
        if (chosen == null) {
            return new Selection(Optional.empty(), applicableMethods, null);
        }
        return new Selection(Optional.of(invocationSnapshot(system, expr, chosen, args, explicitTypeArguments, constraint, extra)), applicableMethods, chosen.candidate().method());
    }

    private static JTypeConstraints.Compatible.Context context(Phase phase) {
        return phase == Phase.STRICT ? JTypeConstraints.Compatible.Context.STRICT_INVOCATION : JTypeConstraints.Compatible.Context.LOOSE_INVOCATION;
    }

    private static boolean potentiallyApplicable(JTypeSystem system, JInvocationCandidate candidate, List<JExpressionInformation> args, List<JArgumentType> explicit, Phase phase) {
        int n = candidate.formals().size();
        int k = args.size();
        if (phase == Phase.VARARG) {
            // JLS §15.12.2.1 variable arity: k >= n - 1
            if (!candidate.variableArity() || k < n - 1 || !(candidate.formals().get(n - 1) instanceof JArrayType)) {
                return false;
            }
        } else if (n != k) {
            return false;
        }
        if (!explicit.isEmpty() && !candidate.explicitlyTypedParameters().isEmpty() && explicit.size() != candidate.explicitlyTypedParameters().size()) {
            // JLS §15.12.2.1 explicit type arguments must match the number of type parameters
            return false;
        }
        List<JType> formals = expandedFormals(candidate.formals(), k, phase);
        for (int i = 0; i < k; i++) {
            if (!potentiallyCompatible(system, args.get(i), formals.get(i), candidate)) {
                return false;
            }
        }
        return true;
    }

    private static boolean potentiallyCompatible(JTypeSystem system, JExpressionInformation arg, JType formal, JInvocationCandidate candidate) {
        if (arg instanceof JExpressionInformation.Lambda lambda) {
            // JLS §15.12.2.1 a lambda is potentially compatible with a functional interface of matching arity
            if (formal instanceof JVarType vt && candidate.typeParameters().contains(vt)) {
                return true;
            }
            Optional<JMethodReference> sam = JReduceLambdaExpression.findFunctionalMethod(formal);
            if (sam.isEmpty() || sam.get().parameters().size() != lambda.parameterCount()) {
                return false;
            }
            boolean voidResult = sam.get().returnType().typeEquals(system.constants().voidType());
            if (lambda.lambdaBody() instanceof JExpressionInformation.LambdaBody.Expression e) {
                // JLS §15.12.2.1 a void function type needs a statement expression body
                return !voidResult || JReduceLambdaExpression.isStatementExpression(e.expression());
            } else if (lambda.lambdaBody() instanceof JExpressionInformation.LambdaBody.Block b) {
                // JLS §15.12.2.1 a block body must be void-compatible or value-compatible to match the result
                return voidResult ? b.isVoidCompatible() : b.isValueCompatible();
            }
            return true;
        } else if (arg instanceof JExpressionInformation.InvocationReference<?> || arg instanceof JExpressionInformation.InstantiationReference) {
            if (formal instanceof JVarType vt && candidate.typeParameters().contains(vt)) {
                return true;
            }
            Optional<JMethodReference> sam = JReduceLambdaExpression.findFunctionalMethod(formal);
            return sam.isPresent() && potentiallyCompatibleReference(system, arg, sam.get().parameters().size());
        } else if (arg instanceof JExpressionInformation.Conditional c) {
            return potentiallyCompatible(system, c.whenTrue(), formal, candidate) && potentiallyCompatible(system, c.whenFalse(), formal, candidate);
        } else if (arg instanceof JExpressionInformation.Switch sw) {
            // JLS §15.12.2.1 a switch expression is potentially compatible if all its result expressions are
            return sw.results().stream().allMatch(r -> potentiallyCompatible(system, r, formal, candidate));
        }
        return true;
    }

    private static boolean potentiallyCompatibleReference(JTypeSystem system, JExpressionInformation arg, int n) {
        if (arg instanceof JExpressionInformation.InstantiationReference ref) {
            if (ref.type() instanceof JArrayType) {
                // JLS §15.13.1 an array constructor reference takes one argument
                return n == 1;
            }
            if (!(ref.type() instanceof JClassType ct)) {
                return true;
            }
            JClassReference declaring = ref instanceof JExpressionInformation.InstantiationReference.Full full ? full.declaring() : null;
            List<JArgumentType> explicit = ref.explicitTypeArguments() == null ? List.of() : ref.explicitTypeArguments();
            // JLS §15.12.2.1 some constructor is potentially applicable with arity n
            return ct.classReference().declaredMethods().stream()
                    .filter(m -> m.location().isConstructor() && accessible(m, declaring))
                    .anyMatch(m -> supportsArity(m, n) && typeArgumentsMatch(m, explicit));
        }
        JExpressionInformation.InvocationReference<?> ref = (JExpressionInformation.InvocationReference<?>) arg;
        JClassReference declaring = ref instanceof JExpressionInformation.InvocationReference.Full<?> full ? full.declaring() : null;
        List<JArgumentType> explicit = ref.explicitTypeArguments() == null ? List.of() : ref.explicitTypeArguments();
        List<JType> sites = new ArrayList<>();
        boolean typeForm = false;
        if (ref.source() instanceof JClassType ct) {
            sites.add(ct);
            typeForm = true;
        } else if (ref.source() instanceof JExpressionInformation.SuperReference) {
            return true;
        } else if (ref.source() instanceof JExpressionInformation expr) {
            JStandaloneTyping.resolve(expr, system).forEach(t -> sites.add(t.type()));
            if (sites.isEmpty()) {
                return true;
            }
        } else {
            return true;
        }
        for (JType site : sites) {
            for (JMethodLookup.Member member : JMethodLookup.members(site, ref.methodName())) {
                JMethodReference m = member.method();
                if (!accessible(m, declaring) || !typeArgumentsMatch(m, explicit)) {
                    continue;
                }
                boolean isStatic = m.hasModifier(AccessFlag.STATIC);
                if (typeForm) {
                    // JLS §15.12.2.1 ReferenceType::m needs a static method of arity n or an instance method of arity n-1
                    if (isStatic ? supportsArity(m, n) : n > 0 && supportsArity(m, n - 1)) {
                        return true;
                    }
                } else if (!isStatic && supportsArity(m, n)) {
                    // JLS §15.12.2.1 other forms need an instance method of arity n
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean accessible(JMethodReference m, JClassReference declaring) {
        return declaring == null || m.outerClass().accessFrom(declaring, null).canAccess(m.access());
    }

    private static boolean supportsArity(JMethodReference m, int n) {
        int count = m.parameters().size();
        return m.hasModifier(AccessFlag.VARARGS) ? n >= count - 1 : n == count;
    }

    private static boolean typeArgumentsMatch(JMethodReference m, List<JArgumentType> explicit) {
        // JLS §15.12.2.1 explicit type arguments must match the number of type parameters of a generic method
        return explicit.isEmpty() || m.typeParameters().isEmpty() || m.typeParameters().size() == explicit.size();
    }

    private static List<JType> expandedFormals(List<JType> formals, int k, Phase phase) {
        if (phase != Phase.VARARG) {
            return formals;
        }
        List<JType> result = new ArrayList<>(formals.subList(0, formals.size() - 1));
        JType component = ((JArrayType) formals.getLast()).component();
        while (result.size() < k) {
            result.add(component);
        }
        return result;
    }

    private static Instantiated instantiate(JTypeSystem system, JInvocationCandidate candidate, List<JArgumentType> explicit, int k, Phase phase) {
        Map<JVarType, JType> theta = new LinkedHashMap<>();
        List<Constraint> constraints = new ArrayList<>();
        boolean useExplicit = !explicit.isEmpty() && explicit.size() == candidate.explicitlyTypedParameters().size() && !candidate.explicitlyTypedParameters().isEmpty();
        for (JVarType p : candidate.typeParameters()) {
            int explicitIndex = candidate.explicitlyTypedParameters().indexOf(p);
            if (useExplicit && explicitIndex >= 0) {
                theta.put(p, explicit.get(explicitIndex));
            } else {
                JMetaVarType alpha = p.createMetaVar();
                theta.put(p, alpha);
                constraints.add(new JTypeConstraints.Infer(alpha, p));
            }
        }
        JVarTypeResolveVisitor resolver = new JVarTypeResolveVisitor(theta);
        if (useExplicit) {
            for (int i = 0; i < candidate.explicitlyTypedParameters().size(); i++) {
                JVarType p = candidate.explicitlyTypedParameters().get(i);
                for (JType bound : JTypeRelations.upperBounds(p)) {
                    // JLS §15.12.2.2 explicit type arguments must be within their bounds
                    constraints.add(new JTypeConstraints.Subtype(explicit.get(i), resolver.visit(bound)));
                }
            }
        }
        for (JType thrown : candidate.thrown()) {
            if (thrown instanceof JVarType vt && theta.get(vt) instanceof JMetaVarType alpha) {
                // JLS §18.1.3 throws α
                constraints.add(new JTypeConstraints.Throws(alpha));
            }
        }
        List<JType> formals = new ArrayList<>();
        for (JType f : expandedFormals(candidate.formals(), k, phase)) {
            formals.add(resolver.visit(f));
        }
        return new Instantiated(theta, constraints, formals);
    }

    private static Optional<Tested> testApplicable(JTypeSystem system, JInvocationCandidate candidate, List<JExpressionInformation> args, List<JArgumentType> explicit, Phase phase) {
        Instantiated inst = instantiate(system, candidate, explicit, args.size(), phase);
        List<Constraint> bind = new ArrayList<>(inst.constraints());
        for (int i = 0; i < args.size(); i++) {
            if (isPertinent(system, args.get(i), candidate.formals().size() > i ? expandedFormals(candidate.formals(), args.size(), phase).get(i) : null, candidate, explicit)) {
                // JLS §18.5.1 ‹ei → Fi θ› for arguments pertinent to applicability
                bind.add(new JTypeConstraints.ExpressionCompatible(args.get(i), context(phase), inst.formals().get(i)));
            }
        }
        if (bind.isEmpty()) {
            return Optional.of(new Tested(false, null, inst.theta()));
        }
        ConstraintTree tree = system.operations().inferenceSolver().bind(bind.toArray(Constraint[]::new)).solve();
        Optional<ConstraintBranch> ok = tree.branches().stream().filter(b -> b.status().isTrue()).findFirst();
        if (ok.isEmpty()) {
            return Optional.empty();
        }
        boolean unchecked = ok.get().constraints().entrySet().stream().anyMatch(e -> e.getValue().isTrue() && e.getKey() instanceof JTypeConstraints.UncheckedConversion uc
                && inst.formals().stream().anyMatch(f -> f.typeEquals(uc.right()) || (f instanceof JArrayType fa && fa.deepComponent().typeEquals(uc.right()))));
        return Optional.of(new Tested(unchecked, ok.get(), inst.theta()));
    }

    public static boolean isPertinent(JTypeSystem system, JExpressionInformation arg, JType formal, JInvocationCandidate candidate, List<JArgumentType> explicit) {
        boolean genericWithoutExplicit = !candidate.typeParameters().isEmpty() && explicit.isEmpty();
        if (arg instanceof JExpressionInformation.Lambda lambda) {
            boolean explicitlyTyped = lambda.parameterCount() == 0 || (lambda.explicitParameterTypes() != null && lambda.explicitParameterTypes().size() == lambda.parameterCount());
            if (!explicitlyTyped) {
                // JLS §15.12.2.2 an implicitly typed lambda is not pertinent to applicability
                return false;
            }
            // JLS §15.12.2.2 an explicitly typed lambda targeting a type parameter of m is not pertinent
            return !(genericWithoutExplicit && formal != null && mentionsAny(formal, candidate.typeParameters()));
        } else if (arg instanceof JExpressionInformation.InvocationReference<?> || arg instanceof JExpressionInformation.InstantiationReference) {
            if (!JResolveMostSpecificMethod.isExactMethodReference(system, arg)) {
                // JLS §15.12.2.2 an inexact method reference is not pertinent to applicability
                return false;
            }
            return !(genericWithoutExplicit && formal != null && mentionsAny(formal, candidate.typeParameters()));
        } else if (arg instanceof JExpressionInformation.Conditional c) {
            return isPertinent(system, c.whenTrue(), formal, candidate, explicit) && isPertinent(system, c.whenFalse(), formal, candidate, explicit);
        }
        return true;
    }

    private static boolean mentionsAny(JType type, List<JVarType> vars) {
        if (vars.isEmpty()) {
            return false;
        }
        return !new JRecursiveTypeVisitor<JType, Void>((JTypeVisitor.Default) (t, c) -> t instanceof JVarType vt && vars.contains(vt) ? vt : null, null, false)
                .visit(type, new HashMap<>()).isEmpty();
    }

    private static Applicable mostSpecific(JTypeSystem system, JExpressionInformation.Invocation expr, List<Applicable> applicable) {
        if (applicable.size() == 1) {
            return applicable.getFirst();
        }
        List<JTypeContext.ChosenMethod> chosen = new ArrayList<>();
        for (Applicable a : applicable) {
            chosen.add(new JTypeContext.ChosenMethod(expr, a.candidate().method(), context(a.phase()), a.phase() == Phase.VARARG));
        }
        List<Integer> maximal = new ArrayList<>();
        for (int i = 0; i < chosen.size(); i++) {
            boolean isMaximal = true;
            for (int j = 0; j < chosen.size(); j++) {
                if (i != j && JResolveMostSpecificMethod.isMoreSpecific(system, chosen.get(j), chosen.get(i)) && !JResolveMostSpecificMethod.isMoreSpecific(system, chosen.get(i), chosen.get(j))) {
                    // JLS §15.12.2.5 m_j is strictly more specific than m_i
                    isMaximal = false;
                    break;
                }
            }
            if (isMaximal) {
                maximal.add(i);
            }
        }
        if (maximal.size() == 1) {
            return applicable.get(maximal.getFirst());
        }
        if (maximal.isEmpty()) {
            return null;
        }
        for (int i : maximal) {
            for (int j : maximal) {
                if (!JMethodLookup.isSubsignature(applicable.get(i).candidate().method(), applicable.get(j).candidate().method())) {
                    // JLS §15.12.2.5 maximally specific methods without override-equivalent signatures are ambiguous
                    return null;
                }
            }
        }
        List<Integer> concrete = maximal.stream().filter(i -> {
            JMethodReference m = applicable.get(i).candidate().method();
            return !m.hasModifier(AccessFlag.ABSTRACT) && !(m.outerClass().hasModifier(AccessFlag.INTERFACE) && !m.hasModifier(AccessFlag.STATIC));
        }).toList();
        if (concrete.size() == 1) {
            // JLS §15.12.2.5 exactly one concrete maximally specific method
            return applicable.get(concrete.getFirst());
        } else if (!concrete.isEmpty()) {
            return null;
        }
        for (int i : maximal) {
            JType ri = applicable.get(i).candidate().result();
            boolean mostSpecificReturn = true;
            for (int j : maximal) {
                if (!JTypeRelations.isSubtype(JTypeRelations.erasure(ri), JTypeRelations.erasure(applicable.get(j).candidate().result())) && !(ri.typeEquals(applicable.get(j).candidate().result()))) {
                    mostSpecificReturn = false;
                    break;
                }
            }
            if (mostSpecificReturn) {
                // JLS §15.12.2.5 all abstract or default: the one with the most specific return type
                return applicable.get(i);
            }
        }
        return null;
    }

    private static JType earlyResolution(JTypeSystem system, Applicable chosen, JType declaredResult, JType target) {
        if (!(declaredResult instanceof JVarType p) || chosen.b2() == null || !(chosen.theta().get(p) instanceof JMetaVarType alpha)) {
            return null;
        }
        ConstraintBranch b2 = chosen.b2();
        List<JType> equal = new ArrayList<>();
        List<JType> lower = new ArrayList<>();
        b2.constraints().forEach((con, status) -> {
            if (!status.isTrue()) {
                return;
            }
            if (con instanceof JTypeConstraints.Equal eq) {
                if (eq.left().typeEquals(alpha) && !(eq.right() instanceof JMetaVarType)) {
                    equal.add(eq.right());
                } else if (eq.right().typeEquals(alpha) && !(eq.left() instanceof JMetaVarType)) {
                    equal.add(eq.left());
                }
            } else if (con instanceof JTypeConstraints.Subtype st && st.right().typeEquals(alpha) && !(st.left() instanceof JMetaVarType)) {
                lower.add(st.left());
            }
        });
        List<JType> sources = new ArrayList<>(equal);
        sources.addAll(lower);
        boolean trigger = false;
        if (target instanceof honeyroasted.jype.type.JPrimitiveType) {
            // JLS §18.5.2.1 a primitive target and a wrapper class bound
            JType inst = honeyroasted.jype.system.solver.constraints.inference.JResolveBounds.findInstantiation(alpha, b2);
            List<JType> all = new ArrayList<>(sources);
            if (inst != null) {
                all.add(inst);
            }
            trigger = all.stream().anyMatch(t -> JTypeRelations.unboxedType(t) != null && t instanceof JClassType);
        } else if (target instanceof JClassType tc && tc.hasTypeArguments() && !JTypeRelations.isRaw(tc)) {
            // JLS §18.5.2.1 a bound that only reaches the target's class as a raw type
            trigger = sources.stream().anyMatch(s -> {
                JClassType sup = JTypeRelations.asSuper(s, tc.classReference());
                return sup != null && JTypeRelations.isRaw(sup);
            });
            if (!trigger && !JTypeRelations.hasWildcardArguments(tc)) {
                trigger = wildcardOrConflicting(sources, lower);
            }
        } else if (!(target instanceof JMetaVarType) && target instanceof JClassType) {
            trigger = wildcardOrConflicting(sources, lower);
        }
        if (!trigger) {
            return null;
        }
        return honeyroasted.jype.system.solver.constraints.inference.JResolveBounds.findInstantiation(alpha, b2);
    }

    private static boolean wildcardOrConflicting(List<JType> sources, List<JType> lower) {
        // JLS §18.5.2.1 (i) a wildcard-parameterized bound
        if (sources.stream().anyMatch(s -> s instanceof JClassType sc && JTypeRelations.hasWildcardArguments(sc))) {
            return true;
        }
        // JLS §18.5.2.1 (ii) two lower bounds with different parameterizations of one generic type
        for (int i = 0; i < lower.size(); i++) {
            for (int j = i + 1; j < lower.size(); j++) {
                for (JClassType a : JTypeRelations.allSupertypeClasses(lower.get(i))) {
                    if (!a.hasTypeArguments()) {
                        continue;
                    }
                    JClassType b = JTypeRelations.asSuper(lower.get(j), a.classReference());
                    if (b != null && b.hasTypeArguments() && !b.typeEquals(a)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static ConstraintBranch.Snapshot invocationSnapshot(JTypeSystem system, JExpressionInformation.Invocation expr, Applicable chosen, List<JExpressionInformation> args,
                                                                List<JArgumentType> explicit, JTypeConstraints.ExpressionCompatible constraint, Map<Constraint, Constraint.Status> extra) {
        JInvocationCandidate candidate = chosen.candidate();
        Instantiated inst = instantiate(system, candidate, explicit, args.size(), chosen.phase());
        Map<Constraint, Constraint.Status> constraints = new LinkedHashMap<>(extra);
        constraints.put(Constraint.identity(java.util.Set.of(candidate.method(), chosen.phase(), expr)), Constraint.Status.TRUE);
        inst.constraints().forEach(c -> constraints.putIfAbsent(c, Constraint.Status.UNKNOWN));
        for (int i = 0; i < args.size(); i++) {
            // JLS §18.5.2.1 ‹ei → Fi θ› for all arguments
            constraints.putIfAbsent(new JTypeConstraints.ExpressionCompatible(args.get(i), context(chosen.phase()), inst.formals().get(i)), Constraint.Status.UNKNOWN);
        }

        JType result;
        if (chosen.unchecked()) {
            // JLS §15.12.2.6 unchecked conversion was necessary: the result type is erased
            result = JTypeRelations.erasure(candidate.result());
        } else {
            result = new JVarTypeResolveVisitor(inst.theta()).visit(candidate.result());
            JType early = earlyResolution(system, chosen, candidate.result(), constraint.right());
            if (early != null && result instanceof JMetaVarType alpha) {
                // JLS §18.5.2.1 α is resolved in B2 before checking compatibility
                constraints.put(new JTypeConstraints.Equal(alpha, early), Constraint.Status.UNKNOWN);
                result = early instanceof JClassType ect && early.isProperType() ? JTypeRelations.capture(ect) : early;
            }
            if (result instanceof JParameterizedClassType pct && JTypeRelations.hasWildcardArguments(pct)) {
                if (result.isProperType()) {
                    // JLS §18.5.2.1 the return type is captured
                    result = JTypeRelations.capture(pct);
                } else {
                    List<JArgumentType> fresh = new ArrayList<>();
                    for (JVarType p : pct.classReference().typeParameters()) {
                        fresh.add(p.createMetaVar());
                    }
                    JParameterizedClassType captured = pct.classReference().parameterized(fresh);
                    // JLS §18.5.2.1 fresh β and the capture bound G<β1..βn> = capture(Rθ)
                    constraints.put(new JTypeConstraints.Capture(captured, pct), Constraint.Status.TRUE);
                    result = captured;
                }
            }
        }
        if (constraint.middle() == JTypeConstraints.Compatible.Context.STRICT_INVOCATION && constraint.right() instanceof honeyroasted.jype.type.JPrimitiveType && expr instanceof JExpressionInformation.MethodInvocation<?>
                && (candidate.result() instanceof JVarType || !(candidate.result() instanceof honeyroasted.jype.type.JPrimitiveType))) {
            // JLS §18.5.2.1 a poly invocation in a strict context is not compatible with a primitive target
            constraints.put(Constraint.FALSE, Constraint.Status.FALSE);
        }
        if (!constraint.right().typeEquals(system.constants().voidType())) {
            // JLS §18.5.2.1 ‹R θ → T›
            constraints.put(new JTypeConstraints.Compatible(result, constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);
        }

        if (expr instanceof JExpressionInformation.Instantiation instantiation && instantiation.anonymous() && !instantiation.type().typeParameters().isEmpty() && instantiation.explicitTypeArguments().isEmpty()) {
            // JLS §15.9.3 an anonymous class created with <> must have denotable inferred type arguments
            constraints.put(new JTypeConstraints.Denotable(result), Constraint.Status.ASSUMED);
        }
        List<JType> thrown = new ArrayList<>();
        JVarTypeResolveVisitor thrownResolver = new JVarTypeResolveVisitor(inst.theta());
        for (JType t : candidate.thrown()) {
            thrown.add(chosen.unchecked() ? JTypeRelations.erasure(t) : thrownResolver.visit(t));
        }
        PropertySet metadata = new PropertySet()
                .attach(new JTypeContext.ThrownTypes(expr, thrown))
                .attach(new JTypeContext.ChosenMethod(expr, candidate.method(), context(chosen.phase()), chosen.phase() == Phase.VARARG))
                .attach(new JTypeContext.InvocationTypeArguments(expr, inst.theta()))
                .attach(new JTypeContext.ExpressionType(expr, result));
        return new ConstraintBranch.Snapshot(metadata, constraints);
    }
}
