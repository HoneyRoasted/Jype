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

    private record Applicable(JInvocationCandidate candidate, Phase phase, boolean unchecked) {
    }

    private record Instantiated(Map<JVarType, JType> theta, List<Constraint> constraints, List<JType> formals) {
    }

    public static Optional<ConstraintBranch.Snapshot> select(JTypeSystem system, JExpressionInformation.Invocation expr, List<JInvocationCandidate> candidates, List<JExpressionInformation> args,
                                                            List<JArgumentType> explicitTypeArguments, JTypeConstraints.ExpressionCompatible constraint, Map<Constraint, Constraint.Status> extra) {
        List<Applicable> applicable = new ArrayList<>();
        for (Phase phase : Phase.values()) {
            for (JInvocationCandidate candidate : candidates) {
                if (potentiallyApplicable(system, candidate, args, explicitTypeArguments, phase)) {
                    testApplicable(system, candidate, args, explicitTypeArguments, phase).ifPresent(unchecked -> applicable.add(new Applicable(candidate, phase, unchecked)));
                }
            }
            if (!applicable.isEmpty()) {
                // JLS §15.12.2 the first phase with an applicable method determines the candidates
                break;
            }
        }
        if (applicable.isEmpty()) {
            return Optional.empty();
        }

        Applicable chosen = mostSpecific(system, expr, applicable);
        if (chosen == null) {
            return Optional.empty();
        }
        return Optional.of(invocationSnapshot(system, expr, chosen, args, explicitTypeArguments, constraint, extra));
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
            return sam.isPresent() && sam.get().parameters().size() == lambda.parameterCount();
        } else if (arg instanceof JExpressionInformation.InvocationReference<?> || arg instanceof JExpressionInformation.InstantiationReference) {
            if (formal instanceof JVarType vt && candidate.typeParameters().contains(vt)) {
                return true;
            }
            return JReduceLambdaExpression.findFunctionalMethod(formal).isPresent();
        } else if (arg instanceof JExpressionInformation.Conditional c) {
            return potentiallyCompatible(system, c.whenTrue(), formal, candidate) && potentiallyCompatible(system, c.whenFalse(), formal, candidate);
        }
        return true;
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

    private static Optional<Boolean> testApplicable(JTypeSystem system, JInvocationCandidate candidate, List<JExpressionInformation> args, List<JArgumentType> explicit, Phase phase) {
        Instantiated inst = instantiate(system, candidate, explicit, args.size(), phase);
        List<Constraint> bind = new ArrayList<>(inst.constraints());
        for (int i = 0; i < args.size(); i++) {
            if (isPertinent(system, args.get(i), candidate.formals().size() > i ? expandedFormals(candidate.formals(), args.size(), phase).get(i) : null, candidate, explicit)) {
                // JLS §18.5.1 ‹ei → Fi θ› for arguments pertinent to applicability
                bind.add(new JTypeConstraints.ExpressionCompatible(args.get(i), context(phase), inst.formals().get(i)));
            }
        }
        if (bind.isEmpty()) {
            return Optional.of(false);
        }
        ConstraintTree tree = system.operations().inferenceSolver().bind(bind.toArray(Constraint[]::new)).solve();
        Optional<ConstraintBranch> ok = tree.branches().stream().filter(b -> b.status().isTrue()).findFirst();
        if (ok.isEmpty()) {
            return Optional.empty();
        }
        boolean unchecked = ok.get().constraints().entrySet().stream().anyMatch(e -> e.getValue().isTrue() && e.getKey() instanceof JTypeConstraints.UncheckedConversion uc
                && inst.formals().stream().anyMatch(f -> f.typeEquals(uc.right()) || (f instanceof JArrayType fa && fa.deepComponent().typeEquals(uc.right()))));
        return Optional.of(unchecked);
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
        if (!(constraint.right() instanceof honeyroasted.jype.type.JNoneType none && none.typeEquals(system.constants().voidType()) && result.typeEquals(system.constants().voidType()))) {
            // JLS §18.5.2.1 ‹R θ → T›
            constraints.put(new JTypeConstraints.Compatible(result, constraint.middle(), constraint.right()), Constraint.Status.UNKNOWN);
        }

        PropertySet metadata = new PropertySet()
                .attach(new JTypeContext.ChosenMethod(expr, candidate.method(), context(chosen.phase()), chosen.phase() == Phase.VARARG))
                .attach(new JTypeContext.ExpressionType(expr, result));
        return new ConstraintBranch.Snapshot(metadata, constraints);
    }
}
