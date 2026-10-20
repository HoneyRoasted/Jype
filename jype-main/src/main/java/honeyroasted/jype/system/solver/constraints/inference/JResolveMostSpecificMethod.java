package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JMemberLookup;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.solver.constraints.reduction.JReduceLambdaExpression;
import honeyroasted.jype.system.visitor.JTypeVisitors;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Prunes sibling {@link JTypeContext.ChosenMethod}-tagged branches of an already-solved
 * {@link ConstraintTree} down to the JLS §15.12.2 compile-time declaration: the first phase
 * (strict, then loose, then variable arity) with any applicable candidate, and within that
 * phase, the most specific applicable candidate. Must run only after the tree has fully
 * settled, since it inspects final branch status rather than reducing constraints itself.
 */
public class JResolveMostSpecificMethod {

    private JResolveMostSpecificMethod() {
    }

    public static void prune(ConstraintTree tree, JTypeSystem system) {
        List<ConstraintBranch> tagged = tree.branches().stream()
                .filter(b -> b.status().isTrue() && b.metadata().has(JTypeContext.ChosenMethod.class))
                .toList();

        Set<ConstraintBranch> processed = new HashSet<>();

        for (ConstraintBranch branch : tagged) {
            if (!processed.add(branch)) {
                continue;
            }

            JTypeContext.ChosenMethod chosen = chosenOf(branch);
            List<ConstraintBranch> group = tagged.stream()
                    .filter(b -> chosenOf(b).expression().equals(chosen.expression()))
                    .toList();

            processed.addAll(group);

            if (group.size() <= 1) {
                continue;
            }

            pruneGroup(system, group);
        }
    }

    private static void pruneGroup(JTypeSystem system, List<ConstraintBranch> group) {
        int bestPhase = group.stream().mapToInt(b -> phaseRank(chosenOf(b))).min().orElse(0);

        List<ConstraintBranch> samePhase = new ArrayList<>();
        for (ConstraintBranch branch : group) {
            if (phaseRank(chosenOf(branch)) == bestPhase) {
                samePhase.add(branch);
            } else {
                invalidate(branch);
            }
        }

        if (samePhase.size() <= 1) {
            return;
        }

        // A single candidate legitimately produces both a strict- and loose-invocation
        // branch (strict implies loose), so group by the actual chosen method first -
        // those are representations of one candidate, not competing candidates.
        Map<JMethodReference, List<ConstraintBranch>> byMethod = new LinkedHashMap<>();
        for (ConstraintBranch branch : samePhase) {
            byMethod.computeIfAbsent(chosenOf(branch).chosen(), k -> new ArrayList<>()).add(branch);
        }

        if (byMethod.size() <= 1) {
            return;
        }

        List<JMethodReference> methods = new ArrayList<>(byMethod.keySet());
        List<JMethodReference> maximal = new ArrayList<>();
        for (JMethodReference candidate : methods) {
            boolean isMaximal = true;
            JTypeContext.ChosenMethod candidateChosen = chosenOf(byMethod.get(candidate).get(0));
            for (JMethodReference other : methods) {
                if (candidate != other) {
                    JTypeContext.ChosenMethod otherChosen = chosenOf(byMethod.get(other).get(0));
                    if (!isMoreSpecific(system, candidateChosen, otherChosen)) {
                        isMaximal = false;
                        break;
                    }
                }
            }
            if (isMaximal) {
                maximal.add(candidate);
            } else {
                byMethod.get(candidate).forEach(JResolveMostSpecificMethod::invalidate);
            }
        }

        if (maximal.size() > 1) {
            List<JMethodReference> nonAbstract = maximal.stream()
                    .filter(m -> !Modifier.isAbstract(m.modifiers()))
                    .toList();

            JMethodReference winner;
            if (nonAbstract.size() == 1) {
                winner = nonAbstract.get(0);
            } else if (nonAbstract.isEmpty()) {
                winner = maximal.get(0);
            } else {
                winner = null;
            }

            for (JMethodReference candidate : maximal) {
                if (candidate != winner) {
                    byMethod.get(candidate).forEach(JResolveMostSpecificMethod::invalidate);
                }
            }
        }
    }

    private static void invalidate(ConstraintBranch branch) {
        branch.add(Constraint.FALSE, Constraint.Status.FALSE);
        branch.executeChanges();
    }

    private static JTypeContext.ChosenMethod chosenOf(ConstraintBranch branch) {
        return branch.metadata().first(JTypeContext.ChosenMethod.class).get();
    }

    private static int phaseRank(JTypeContext.ChosenMethod cm) {
        if (cm.vararg()) return 2;
        return cm.context() == JTypeConstraints.Compatible.Context.STRICT_INVOCATION ? 0 : 1;
    }

    /**
     * A method's own formal parameter types (vararg-expanded to the call's argument count when
     * applicable), left exactly as declared - even if the method is generic, its own type
     * parameters are never substituted here. Per JLS §18.5.4: "no substitution is applied to
     * S1, ..., Sk; even if m1 is generic, the type parameters of m1 are treated as type
     * variables, not inference variables" - this holds for whichever method is playing the "m1"
     * role in a given pairwise direction, regardless of what its type parameters actually
     * resolved to for this specific call.
     */
    private static List<JType> declaredParameters(JTypeContext.ChosenMethod cm) {
        List<JType> declared = cm.chosen().parameters();
        if (!cm.vararg()) {
            return declared;
        }

        int argCount = cm.expression().parameters().size();
        int fixed = declared.size() - 1;

        List<JType> effective = new ArrayList<>(declared.subList(0, fixed));
        JType lastParam = declared.get(fixed);
        JType component = lastParam instanceof JArrayType at ? at.component() : lastParam;
        for (int i = fixed; i < argCount; i++) {
            effective.add(component);
        }

        return effective;
    }

    /**
     * JLS §15.12.2.5: is {@code m1} more specific than {@code m2}? This is directional - the two
     * arguments are not interchangeable - and is invoked twice (once per direction) by
     * {@link #pruneGroup} to determine the "strictly more specific" relation between a pair of
     * candidates.
     */
    private static boolean isMoreSpecific(JTypeSystem system, JTypeContext.ChosenMethod m1, JTypeContext.ChosenMethod m2) {
        List<JType> s = declaredParameters(m1);
        List<JType> t = declaredParameters(m2);
        List<JExpressionInformation> args = m1.expression().parameters();
        int size = Math.min(s.size(), t.size());

        if (m2.chosen().typeParameters().isEmpty()) {
            // m2 is not generic: Si is more specific than Ti for all i, no inference needed.
            for (int i = 0; i < size; i++) {
                if (!moreSpecificType(system, s.get(i), t.get(i), args.get(i))) {
                    return false;
                }
            }
            return true;
        }

        return isMoreSpecificGeneric(system, s, m2.chosen(), t, args, size);
    }

    /**
     * JLS §18.5.4 ("More Specific Method Inference"): m2 is generic, so testing whether m1 is
     * more specific requires inferring an instantiation of m2's own type parameters (P1...Pp)
     * under fresh inference variables (α1...αp) that makes m1 more specific than the
     * θ-substituted m2, where θ = [P1:=α1, ..., Pp:=αp].
     */
    private static boolean isMoreSpecificGeneric(JTypeSystem system, List<JType> s, JMethodReference m2, List<JType> declaredT, List<JExpressionInformation> args, int size) {
        Map<JVarType, JType> substitution = new LinkedHashMap<>();
        List<Constraint> constraints = new ArrayList<>();

        // Initial bound set B, generated from the declared bounds of P1, ..., Pp (JLS §18.1.3):
        // each Infer constraint below is picked up by JBuildInitialBounds inside inferenceSolver().
        for (JVarType p : m2.typeParameters()) {
            JMetaVarType alpha = p.createMetaVar();
            substitution.put(p, alpha);
            constraints.add(new JTypeConstraints.Infer(alpha, p));
        }
        JVarTypeResolveVisitor theta = new JVarTypeResolveVisitor(substitution);

        for (int i = 0; i < size; i++) {
            JType si = s.get(i);
            JType ti = theta.apply(declaredT.get(i));

            if (ti.isProperType()) {
                // Ti mentions none of m2's type parameters: no inference needed for this
                // position, the result is simply whether Si is more specific than Ti.
                if (!moreSpecificType(system, si, ti, args.get(i))) {
                    return false;
                }
            } else if (isFunctionalInterfaceType(si) && isFunctionalInterfaceType(ti) && !interfacesRelated(system, si, ti)) {
                // JLS §18.5.4's own elaborate MTS/MTT bullet: Ti isn't proper (it mentions one of
                // m2's still-unresolved type parameters), so unlike moreSpecificType's plain
                // isSubtype-or-functional-interface-comparison decision, the function-type
                // comparison itself has to be expressed as constraints fed into the same nested
                // solve as everything else here, rather than decided immediately.
                if (!generateFunctionalInterfaceConstraints(system, si, ti, args.get(i), constraints)) {
                    return false;
                }
            } else {
                constraints.add(new JTypeConstraints.Subtype(si, ti));
            }
        }

        // Fourth: reduce/incorporate the generated bounds with B to produce B'. m1 is more
        // specific than m2 iff B' contains no false bound and resolution of all inference
        // variables in B' succeeds - i.e. at least one branch of this nested solve is true.
        ConstraintTree solved = system.operations().inferenceSolver()
                .bind(constraints.toArray(new Constraint[0]))
                .solve();
        return solved.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    /**
     * JLS §15.12.2.5: "A type S is more specific than a type T for any expression if S <: T" -
     * plus the functional-interface-specific refinement defined a few paragraphs later, which
     * compares S/T's function types (JLS §9.9) instead of a plain S <: T whenever both are
     * functional interface types and neither's interface is a super/subinterface of the other's
     * (in that related case, plain S <: T already gives the right answer).
     */
    private static boolean moreSpecificType(JTypeSystem system, JType s, JType t, JExpressionInformation expr) {
        if (isFunctionalInterfaceType(s) && isFunctionalInterfaceType(t) && !interfacesRelated(system, s, t)) {
            return moreSpecificFunctionalInterface(system, s, t, expr);
        }

        return system.operations().isSubtype(s, t);
    }

    /**
     * Whether S's and T's underlying interfaces are related by inheritance at all, ignoring type
     * arguments (JLS: "the interface of S is neither a superinterface nor a subinterface of the
     * interface of T"). Uses erasure specifically to strip type arguments before checking, since
     * the raw interface identity - not the parameterization - is what this bullet cares about.
     */
    private static boolean interfacesRelated(JTypeSystem system, JType s, JType t) {
        JType erasedS = JTypeVisitors.ERASURE.visit(s);
        JType erasedT = JTypeVisitors.ERASURE.visit(t);
        return system.operations().isSubtype(erasedS, erasedT) || system.operations().isSubtype(erasedT, erasedS);
    }

    private static boolean isFunctionalInterfaceType(JType type) {
        return JReduceLambdaExpression.findFunctionalMethod(type).isPresent();
    }

    /**
     * JLS §15.12.2.5's functional-interface comparison: match the two SAMs' type parameter
     * counts and arity, then compare their function types' type-parameter bounds, formal
     * parameters, and return type.
     *
     * Models capture conversion of S properly (MTS = function type of the *capture* of S, MTS' =
     * function type of S without capture - see {@link #captureConvert}), mirroring
     * {@link #generateFunctionalInterfaceConstraints}'s treatment of the same distinction for the
     * §18.5.4 path; the only difference here is that every comparison below is an immediate
     * boolean decision rather than a generated constraint, since this path is only reached once T
     * is already a proper type (no unresolved inference variables left to solve for).
     */
    private static boolean moreSpecificFunctionalInterface(JTypeSystem system, JType s, JType t, JExpressionInformation expr) {
        JType capturedS = captureConvert(system, s, new ArrayList<>());
        Optional<JMethodReference> mtsOpt = JReduceLambdaExpression.findFunctionalMethod(capturedS);
        Optional<JMethodReference> mtsPrimeOpt = JReduceLambdaExpression.findFunctionalMethod(s);
        Optional<JMethodReference> mttOpt = JReduceLambdaExpression.findFunctionalMethod(t);
        if (mtsOpt.isEmpty() || mtsPrimeOpt.isEmpty() || mttOpt.isEmpty()) return false;

        JMethodReference mts = mtsOpt.get();
        JMethodReference mtsPrime = mtsPrimeOpt.get();
        JMethodReference mtt = mttOpt.get();

        if (mts.typeParameters().size() != mtt.typeParameters().size()) return false;
        if (mts.parameters().size() != mtt.parameters().size()) return false;
        if (mts.typeParameters().size() != mtsPrime.typeParameters().size()) return false;

        // θ' = [B1:=A1, ..., Bn:=An]: map MTT's own type parameters onto MTS's.
        Map<JVarType, JType> thetaPrime = new LinkedHashMap<>();
        for (int j = 0; j < mtt.typeParameters().size(); j++) {
            thetaPrime.put(mtt.typeParameters().get(j), mts.typeParameters().get(j));
        }
        JVarTypeResolveVisitor thetaPrimeResolver = new JVarTypeResolveVisitor(thetaPrime);

        // Type parameters' own declared bounds must line up under θ' (in practice almost always
        // vacuous - a functional interface's SAM essentially never declares its own generic
        // method type parameters).
        for (int j = 0; j < mtt.typeParameters().size(); j++) {
            JType boundA = mts.typeParameters().get(j).upperBound();
            JType boundB = thetaPrimeResolver.apply(mtt.typeParameters().get(j).upperBound());
            if (!boundA.typeEquals(boundB)) return false;
        }

        // θ'' = [B1:=A1', ..., Bn:=An']: map MTT's type parameters onto MTS' (uncaptured) ones,
        // for the capture-sensitive half of the formal-parameter comparison below.
        Map<JVarType, JType> thetaDoublePrime = new LinkedHashMap<>();
        for (int j = 0; j < mtt.typeParameters().size(); j++) {
            thetaDoublePrime.put(mtt.typeParameters().get(j), mtsPrime.typeParameters().get(j));
        }
        JVarTypeResolveVisitor thetaDoublePrimeResolver = new JVarTypeResolveVisitor(thetaDoublePrime);

        for (int j = 0; j < mts.parameters().size(); j++) {
            JType uj = mts.parameters().get(j);
            JType vj = thetaPrimeResolver.apply(mtt.parameters().get(j));
            if (!system.operations().isSubtype(vj, uj)) return false;

            JType ujPrime = mtsPrime.parameters().get(j);
            JType vjPrime = thetaDoublePrimeResolver.apply(mtt.parameters().get(j));
            if (!vjPrime.typeEquals(ujPrime)) return false;
        }

        JType rs = mts.returnType();
        JType rt = thetaPrimeResolver.apply(mtt.returnType());
        return moreSpecificReturnType(system, rs, rt, expr);
    }

    /**
     * JLS §15.12.2.5's return-type step of the functional-interface comparison: mostly a plain
     * RS <: RT, refined by the shape of the actual argument expression at this position.
     * Switch-expression recursion is not modeled (this codebase doesn't model switch expressions
     * at all yet) - it falls back to the plain subtype check, same as any other expression shape
     * the literal spec doesn't special-case.
     */
    private static boolean moreSpecificReturnType(JTypeSystem system, JType rs, JType rt, JExpressionInformation expr) {
        if (expr instanceof JExpressionInformation.Lambda lambda && isExplicitlyTyped(lambda)) {
            if (rt.typeEquals(system.constants().voidType())) return true;

            JExpressionInformation result = lambda.body();

            Optional<JMethodReference> rsSam = JReduceLambdaExpression.findFunctionalMethod(rs);
            Optional<JMethodReference> rtSam = JReduceLambdaExpression.findFunctionalMethod(rt);
            if (rsSam.isPresent() && rtSam.isPresent()) {
                return moreSpecificType(system, rs, rt, result);
            }

            boolean rsPrimitive = rs instanceof JPrimitiveType;
            boolean rtPrimitive = rt instanceof JPrimitiveType;
            if (rsPrimitive && !rtPrimitive && isStandalonePrimitive(system, result)) {
                return true;
            }
            if (rtPrimitive && !rsPrimitive && isStandaloneReferenceOrPoly(system, result)) {
                return true;
            }

            return system.operations().isSubtype(rs, rt);
        }

        if (isExactMethodReference(system, expr)) {
            if (rt.typeEquals(system.constants().voidType())) return true;

            boolean rsPrimitive = rs instanceof JPrimitiveType;
            boolean rtPrimitive = rt instanceof JPrimitiveType;
            Boolean declarationReturnsPrimitive = exactMethodReferenceReturnsPrimitive(system, expr);
            if (declarationReturnsPrimitive != null) {
                if (rsPrimitive && !rtPrimitive && declarationReturnsPrimitive) return true;
                if (rtPrimitive && !rsPrimitive && !declarationReturnsPrimitive) return true;
            }

            return system.operations().isSubtype(rs, rt);
        }

        if (expr instanceof JExpressionInformation.Conditional cond) {
            return moreSpecificReturnType(system, rs, rt, cond.whenTrue())
                    && moreSpecificReturnType(system, rs, rt, cond.whenFalse());
        }

        return system.operations().isSubtype(rs, rt);
    }

    private static boolean isExplicitlyTyped(JExpressionInformation.Lambda lambda) {
        return lambda.explicitParameterTypes() != null && lambda.explicitParameterTypes().size() == lambda.parameterCount();
    }

    private static boolean isStandalonePrimitive(JTypeSystem system, JExpressionInformation expr) {
        if (!expr.isSimplyTyped()) return false;
        return expr.getSimpleType(system, Function.identity()).map(t -> t instanceof JPrimitiveType).orElse(false);
    }

    private static boolean isStandaloneReferenceOrPoly(JTypeSystem system, JExpressionInformation expr) {
        if (!expr.isSimplyTyped()) return true;
        return expr.getSimpleType(system, Function.identity()).map(t -> !(t instanceof JPrimitiveType)).orElse(false);
    }

    /**
     * JLS §18.5.4's own elaborate MTS/MTT bullet, reached when Si and Ti are both functional
     * interface types with unrelated interfaces and Ti is *not* proper (i.e. it mentions one of
     * m2's still-unresolved type parameters) - so unlike {@link #moreSpecificFunctionalInterface},
     * this can't decide the comparison immediately and instead generates equality/subtype
     * constraint formulas into {@code constraints}, to be resolved together with everything else
     * in the same nested {@link #isMoreSpecificGeneric} solve.
     *
     * Unlike {@link #moreSpecificFunctionalInterface} (which treats MTS and MTS' as identical),
     * this models capture conversion of Si properly via {@link #captureConvert}: MTS is the
     * function type of the *captured* Si, MTS' is the function type of Si without capture.
     *
     * Simplification (matching {@link #moreSpecificFunctionalInterface}'s own documented
     * approach): the spec's "if the bound of Aj/Uj/RS mentions one of MTS's own type parameters
     * and the corresponding MTT part is not a proper type, false" escape hatches aren't modeled -
     * an equality/subtype constraint is generated unconditionally instead. This only matters for a
     * SAM method that is itself generic (not merely the enclosing functional interface), which is
     * vanishingly rare in practice.
     */
    private static boolean generateFunctionalInterfaceConstraints(JTypeSystem system, JType si, JType ti, JExpressionInformation expr, List<Constraint> constraints) {
        JType capturedSi = captureConvert(system, si, constraints);
        Optional<JMethodReference> mtsOpt = JReduceLambdaExpression.findFunctionalMethod(capturedSi);
        Optional<JMethodReference> mtsPrimeOpt = JReduceLambdaExpression.findFunctionalMethod(si);
        Optional<JMethodReference> mttOpt = JReduceLambdaExpression.findFunctionalMethod(ti);
        if (mtsOpt.isEmpty() || mtsPrimeOpt.isEmpty() || mttOpt.isEmpty()) return false;

        JMethodReference mts = mtsOpt.get();
        JMethodReference mtsPrime = mtsPrimeOpt.get();
        JMethodReference mtt = mttOpt.get();

        if (mts.typeParameters().size() != mtt.typeParameters().size()) return false;
        if (mts.parameters().size() != mtt.parameters().size()) return false;
        if (mts.typeParameters().size() != mtsPrime.typeParameters().size()) return false;

        // θ' = [B1:=A1, ..., Bn:=An]: map MTT's own type parameters onto MTS's.
        Map<JVarType, JType> thetaPrime = new LinkedHashMap<>();
        for (int j = 0; j < mtt.typeParameters().size(); j++) {
            thetaPrime.put(mtt.typeParameters().get(j), mts.typeParameters().get(j));
        }
        JVarTypeResolveVisitor thetaPrimeResolver = new JVarTypeResolveVisitor(thetaPrime);

        for (int j = 0; j < mtt.typeParameters().size(); j++) {
            JType boundA = mts.typeParameters().get(j).upperBound();
            JType boundB = thetaPrimeResolver.apply(mtt.typeParameters().get(j).upperBound());
            constraints.add(new JTypeConstraints.Equal(boundA, boundB));
        }

        // θ'' = [B1:=A1', ..., Bn:=An']: map MTT's type parameters onto MTS' (uncaptured) ones,
        // for the capture-sensitive half of the formal-parameter comparison below.
        Map<JVarType, JType> thetaDoublePrime = new LinkedHashMap<>();
        for (int j = 0; j < mtt.typeParameters().size(); j++) {
            thetaDoublePrime.put(mtt.typeParameters().get(j), mtsPrime.typeParameters().get(j));
        }
        JVarTypeResolveVisitor thetaDoublePrimeResolver = new JVarTypeResolveVisitor(thetaDoublePrime);

        for (int j = 0; j < mts.parameters().size(); j++) {
            JType uj = mts.parameters().get(j);
            JType vj = thetaPrimeResolver.apply(mtt.parameters().get(j));
            constraints.add(new JTypeConstraints.Subtype(vj, uj));

            JType ujPrime = mtsPrime.parameters().get(j);
            JType vjPrime = thetaDoublePrimeResolver.apply(mtt.parameters().get(j));
            constraints.add(new JTypeConstraints.Equal(vjPrime, ujPrime));
        }

        JType rs = mts.returnType();
        JType rt = thetaPrimeResolver.apply(mtt.returnType());
        return generateReturnTypeConstraints(system, rs, rt, expr, constraints);
    }

    /**
     * Constraint-generating counterpart of {@link #moreSpecificReturnType}, used when RT may
     * still mention m2's unresolved inference variables (so the comparison can't be decided
     * immediately) - same expression-shape dispatch, but the terminal "RS <: RT" case is emitted
     * as a {@link JTypeConstraints.Subtype} constraint instead of an immediate
     * {@code isSubtype} check.
     */
    private static boolean generateReturnTypeConstraints(JTypeSystem system, JType rs, JType rt, JExpressionInformation expr, List<Constraint> constraints) {
        if (expr instanceof JExpressionInformation.Lambda lambda && isExplicitlyTyped(lambda)) {
            if (rt.typeEquals(system.constants().voidType())) return true;

            JExpressionInformation result = lambda.body();

            Optional<JMethodReference> rsSam = JReduceLambdaExpression.findFunctionalMethod(rs);
            Optional<JMethodReference> rtSam = JReduceLambdaExpression.findFunctionalMethod(rt);
            if (rsSam.isPresent() && rtSam.isPresent()) {
                return generateFunctionalInterfaceConstraints(system, rs, rt, result, constraints);
            }

            boolean rsPrimitive = rs instanceof JPrimitiveType;
            boolean rtPrimitive = rt instanceof JPrimitiveType;
            if (rsPrimitive && !rtPrimitive && isStandalonePrimitive(system, result)) {
                return true;
            }
            if (rtPrimitive && !rsPrimitive && isStandaloneReferenceOrPoly(system, result)) {
                return true;
            }

            constraints.add(new JTypeConstraints.Subtype(rs, rt));
            return true;
        }

        if (isExactMethodReference(system, expr)) {
            if (rt.typeEquals(system.constants().voidType())) return true;

            boolean rsPrimitive = rs instanceof JPrimitiveType;
            boolean rtPrimitive = rt instanceof JPrimitiveType;
            Boolean declarationReturnsPrimitive = exactMethodReferenceReturnsPrimitive(system, expr);
            if (declarationReturnsPrimitive != null) {
                if (rsPrimitive && !rtPrimitive && declarationReturnsPrimitive) return true;
                if (rtPrimitive && !rsPrimitive && !declarationReturnsPrimitive) return true;
            }

            constraints.add(new JTypeConstraints.Subtype(rs, rt));
            return true;
        }

        if (expr instanceof JExpressionInformation.Conditional cond) {
            return generateReturnTypeConstraints(system, rs, rt, cond.whenTrue(), constraints)
                    && generateReturnTypeConstraints(system, rs, rt, cond.whenFalse(), constraints);
        }

        constraints.add(new JTypeConstraints.Subtype(rs, rt));
        return true;
    }

    /**
     * JLS §5.1.10 capture conversion, applied directly at the type level: each wildcard type
     * argument of a parameterized class type is replaced with a fresh type variable bounded per
     * the wildcard's own bound and the class's declared type-parameter bound, exactly as the
     * spec defines it - independent of any constraint-branch/solver machinery.
     *
     * This is deliberately *not* {@link JMemberLookup#captureConvert}: that helper mints
     * unbounded fresh metavariables and relies on {@code JIncorporationCapture} to derive their
     * bounds reactively from *other* constraints already present in an active branch - there is no
     * such branch here (this runs inside a one-shot, non-solver comparison), and more importantly
     * no such "other constraint" would exist to react to. The bounds needed here are inherent to
     * the capture conversion itself, so they're computed directly, synchronously, by mutating the
     * fresh {@link JMetaVarType}'s own (plain, mutable) bound sets - and, since the nested solve in
     * {@link #isMoreSpecificGeneric} only resolves variables through actual bound
     * constraints (mirroring {@code JBuildInitialBounds}'s treatment of ordinary inference
     * variables), each fresh variable's bound is also emitted into {@code constraints} as a real
     * {@link JTypeConstraints.Subtype} bound, not left as inert data on the type alone.
     */
    static JType captureConvert(JTypeSystem system, JType type, List<Constraint> constraints) {
        if (!(type instanceof JParameterizedClassType pct) || !pct.hasAnyTypeArguments()) {
            return type;
        }

        List<JArgumentType> args = pct.typeArguments();
        if (args.stream().noneMatch(a -> a instanceof JWildType)) {
            return type;
        }

        List<JVarType> declared = pct.typeParameters();
        List<JArgumentType> fresh = new ArrayList<>();
        for (int i = 0; i < declared.size(); i++) {
            JArgumentType arg = i < args.size() ? args.get(i) : null;
            fresh.add(arg instanceof JWildType ? declared.get(i).createMetaVar() : arg);
        }

        Map<JVarType, JType> substitution = new LinkedHashMap<>();
        for (int i = 0; i < declared.size(); i++) {
            substitution.put(declared.get(i), (JType) fresh.get(i));
        }
        JVarTypeResolveVisitor theta = new JVarTypeResolveVisitor(substitution);

        for (int i = 0; i < declared.size() && i < args.size(); i++) {
            if (!(fresh.get(i) instanceof JMetaVarType xi)) continue;

            JType substitutedDeclaredBound = theta.apply(declared.get(i).upperBound());
            JArgumentType ai = args.get(i);
            if (ai instanceof JWildType.Lower wtl) {
                // ? super Bi: upper bound is Ui[theta], lower bound is Bi.
                constraints.add(JTypeConstraints.Subtype.createBound(xi, substitutedDeclaredBound));
                wtl.lowerBounds().forEach(lb -> constraints.add(JTypeConstraints.Subtype.createBound(lb, xi)));
            } else if (ai instanceof JWildType.Upper wtu) {
                // ? extends Bi (or unbounded ?, Bi = Object): upper bound is glb(Bi, Ui[theta]),
                // lower bound is the null type (left as the default empty lowerBounds()).
                constraints.add(JTypeConstraints.Subtype.createBound(xi, substitutedDeclaredBound));
                if (!wtu.hasDefaultBounds()) {
                    wtu.upperBounds().forEach(ub -> constraints.add(JTypeConstraints.Subtype.createBound(xi, ub)));
                }
            }
        }

        return pct.classReference().parameterized(fresh);
    }

    /**
     * JLS §15.13.1: whether a method/constructor reference expression's compile-time declaration
     * can be determined without its target type at all - i.e. exactly one applicable-by-name,
     * non-variable-arity, non-generic-or-explicitly-witnessed candidate exists (or it's an array
     * constructor reference, always exact).
     *
     * Approximation: this codebase doesn't model raw types (see [[project_remaining_scope]] -
     * unchecked-conversion tracking is explicitly out of scope), so the spec's "ReferenceType/
     * ClassType does not denote a raw type" condition is skipped, and accessibility is approximated
     * as "found via the ordinary member-lookup used elsewhere in this file" rather than the full
     * JLS accessibility procedure.
     */
    private static boolean isExactMethodReference(JTypeSystem system, JExpressionInformation expr) {
        if (expr instanceof JExpressionInformation.InstantiationReference instRef && instRef.type() instanceof JArrayType) {
            return true;
        }
        return exactMethodReferenceDeclaration(system, expr).isPresent();
    }

    /**
     * Whether the compile-time declaration an exact method/constructor reference resolves to has
     * a primitive return type - {@code null} if {@code expr} isn't an exact method reference at
     * all (callers should fall back to a plain subtype comparison in that case).
     */
    private static Boolean exactMethodReferenceReturnsPrimitive(JTypeSystem system, JExpressionInformation expr) {
        if (expr instanceof JExpressionInformation.InstantiationReference) {
            // Constructors (including the synthetic T[]::new) never yield a primitive type.
            return false;
        }
        return exactMethodReferenceDeclaration(system, expr)
                .map(m -> m.returnType() instanceof JPrimitiveType)
                .orElse(null);
    }

    private static Optional<JMethodReference> exactMethodReferenceDeclaration(JTypeSystem system, JExpressionInformation expr) {
        if (expr instanceof JExpressionInformation.InvocationReference<?> methodRef) {
            List<JMethodReference> candidates;
            if (methodRef.source() instanceof JClassReference classRef) {
                candidates = JMemberLookup.allMethods(classRef).stream()
                        .filter(m -> m.location().name().equals(methodRef.methodName()))
                        .toList();
            } else if (methodRef.source() instanceof JExpressionInformation sourceExpr && sourceExpr.isSimplyTyped()) {
                Optional<JType> sourceType = sourceExpr.getSimpleType(system, Function.identity());
                if (sourceType.isEmpty()) return Optional.empty();

                List<JMethodReference> found = new ArrayList<>();
                for (JClassType ct : JMemberLookup.findClassTypes(sourceType.get())) {
                    for (JMethodReference m : JMemberLookup.allMethods(ct.classReference())) {
                        if (m.location().name().equals(methodRef.methodName()) && !m.hasModifier(AccessFlag.PRIVATE)) {
                            found.add(m);
                        }
                    }
                }
                candidates = found;
            } else {
                return Optional.empty();
            }

            if (candidates.size() != 1) return Optional.empty();
            JMethodReference m = candidates.get(0);
            if (m.hasModifier(AccessFlag.VARARGS)) return Optional.empty();
            if (!m.typeParameters().isEmpty() && isEmpty(methodRef.explicitTypeArguments())) return Optional.empty();
            return Optional.of(m);
        }

        if (expr instanceof JExpressionInformation.InstantiationReference instRef) {
            if (instRef.type() instanceof JArrayType) {
                // Always exact, but there's no user-declared member to report a declaration for.
                return Optional.empty();
            }
            if (!(instRef.type() instanceof JClassReference classRef)) return Optional.empty();

            List<JMethodReference> ctors = classRef.declaredMethods().stream()
                    .filter(m -> m.location().isConstructor() && !m.hasModifier(AccessFlag.PRIVATE))
                    .toList();
            if (ctors.size() != 1) return Optional.empty();

            JMethodReference ctor = ctors.get(0);
            if (ctor.hasModifier(AccessFlag.VARARGS)) return Optional.empty();
            if (!ctor.typeParameters().isEmpty() && isEmpty(instRef.explicitTypeArguments())) return Optional.empty();
            return Optional.of(ctor);
        }

        return Optional.empty();
    }

    private static boolean isEmpty(List<JArgumentType> typeArguments) {
        return typeArguments == null || typeArguments.isEmpty();
    }
}
