package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JMethodLookup;
import honeyroasted.jype.system.solver.JStandaloneType;
import honeyroasted.jype.system.solver.JStandaloneTyping;
import honeyroasted.jype.system.solver.JTypeRelations;
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
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

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
import java.util.function.Predicate;

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
        if (cm.vararg()) {
            return 2;
        }
        return cm.context() == JTypeConstraints.Compatible.Context.STRICT_INVOCATION ? 0 : 1;
    }

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

    public static boolean isMoreSpecific(JTypeSystem system, JTypeContext.ChosenMethod m1, JTypeContext.ChosenMethod m2) {
        List<JType> s = declaredParameters(m1);
        List<JType> t = declaredParameters(m2);
        List<JExpressionInformation> args = m1.expression().parameters();
        int size = Math.min(s.size(), t.size());

        if (m2.chosen().typeParameters().isEmpty()) {
            for (int i = 0; i < size; i++) {
                if (!moreSpecificType(system, s.get(i), t.get(i), args.get(i))) {
                    return false;
                }
            }
            return true;
        }

        return isMoreSpecificGeneric(system, s, m2.chosen(), t, args, size);
    }

    private static boolean isMoreSpecificGeneric(JTypeSystem system, List<JType> s, JMethodReference m2, List<JType> declaredT, List<JExpressionInformation> args, int size) {
        Map<JVarType, JType> substitution = new LinkedHashMap<>();
        List<Constraint> constraints = new ArrayList<>();

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
                if (!moreSpecificType(system, si, ti, args.get(i))) {
                    return false;
                }
            } else if (isFunctionalInterfaceType(si) && isFunctionalInterfaceType(ti) && !interfacesRelated(system, si, ti)) {
                // JLS §18.5.4's own elaborate MTS/MTT bullet: Ti isn't proper (it mentions one of
                if (!generateFunctionalInterfaceConstraints(system, si, ti, args.get(i), constraints)) {
                    return false;
                }
            } else {
                constraints.add(new JTypeConstraints.Subtype(si, ti));
            }
        }

        ConstraintTree solved = system.operations().inferenceSolver()
                .bind(constraints.toArray(new Constraint[0]))
                .solve();
        return solved.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    private static boolean moreSpecificType(JTypeSystem system, JType s, JType t, JExpressionInformation expr) {
        if (isFunctionalInterfaceType(s) && isFunctionalInterfaceType(t) && !interfacesRelated(system, s, t)) {
            return moreSpecificFunctionalInterface(system, s, t, expr);
        }

        return system.operations().isSubtype(s, t);
    }

    private static boolean interfacesRelated(JTypeSystem system, JType s, JType t) {
        JType erasedS = JTypeVisitors.ERASURE.visit(s);
        JType erasedT = JTypeVisitors.ERASURE.visit(t);
        return system.operations().isSubtype(erasedS, erasedT) || system.operations().isSubtype(erasedT, erasedS);
    }

    private static boolean isFunctionalInterfaceType(JType type) {
        return JReduceLambdaExpression.findFunctionalMethod(type).isPresent();
    }

    private static boolean moreSpecificFunctionalInterface(JTypeSystem system, JType s, JType t, JExpressionInformation expr) {
        JType capturedS = captureConvert(system, s, new ArrayList<>());
        Optional<JMethodReference> mtsOpt = JReduceLambdaExpression.findFunctionalMethod(capturedS);
        Optional<JMethodReference> mtsPrimeOpt = JReduceLambdaExpression.findFunctionalMethod(s);
        Optional<JMethodReference> mttOpt = JReduceLambdaExpression.findFunctionalMethod(t);
        if (mtsOpt.isEmpty() || mtsPrimeOpt.isEmpty() || mttOpt.isEmpty()) {
            return false;
        }

        JMethodReference mts = mtsOpt.get();
        JMethodReference mtsPrime = mtsPrimeOpt.get();
        JMethodReference mtt = mttOpt.get();

        if (mts.typeParameters().size() != mtt.typeParameters().size()) {
            return false;
        }
        if (mts.parameters().size() != mtt.parameters().size()) {
            return false;
        }
        if (mts.typeParameters().size() != mtsPrime.typeParameters().size()) {
            return false;
        }

        Map<JVarType, JType> thetaPrime = new LinkedHashMap<>();
        for (int j = 0; j < mtt.typeParameters().size(); j++) {
            thetaPrime.put(mtt.typeParameters().get(j), mts.typeParameters().get(j));
        }
        JVarTypeResolveVisitor thetaPrimeResolver = new JVarTypeResolveVisitor(thetaPrime);

        for (int j = 0; j < mtt.typeParameters().size(); j++) {
            JType boundA = mts.typeParameters().get(j).upperBound();
            JType boundB = thetaPrimeResolver.apply(mtt.typeParameters().get(j).upperBound());
            if (!boundA.typeEquals(boundB)) {
                return false;
            }
        }

        Map<JVarType, JType> thetaDoublePrime = new LinkedHashMap<>();
        for (int j = 0; j < mtt.typeParameters().size(); j++) {
            thetaDoublePrime.put(mtt.typeParameters().get(j), mtsPrime.typeParameters().get(j));
        }
        JVarTypeResolveVisitor thetaDoublePrimeResolver = new JVarTypeResolveVisitor(thetaDoublePrime);

        for (int j = 0; j < mts.parameters().size(); j++) {
            JType uj = mts.parameters().get(j);
            JType vj = thetaPrimeResolver.apply(mtt.parameters().get(j));
            if (!system.operations().isSubtype(vj, uj)) {
                return false;
            }

            JType ujPrime = mtsPrime.parameters().get(j);
            JType vjPrime = thetaDoublePrimeResolver.apply(mtt.parameters().get(j));
            if (!vjPrime.typeEquals(ujPrime)) {
                return false;
            }
        }

        JType rs = mts.returnType();
        JType rt = thetaPrimeResolver.apply(mtt.returnType());
        return moreSpecificReturnType(system, rs, rt, expr);
    }

    private static boolean moreSpecificReturnType(JTypeSystem system, JType rs, JType rt, JExpressionInformation expr) {
        if (expr instanceof JExpressionInformation.Conditional cond) {
            // JLS §15.12.2.5 a conditional expression applies recursively to both operands
            return moreSpecificReturnType(system, rs, rt, cond.whenTrue()) && moreSpecificReturnType(system, rs, rt, cond.whenFalse());
        }
        if (expr instanceof JExpressionInformation.Lambda lambda && isExplicitlyTyped(lambda)) {
            // JLS §15.12.2.5 explicitly typed lambda
            if (rt.typeEquals(system.constants().voidType())) {
                return true;
            }
            if (rs.typeEquals(system.constants().voidType())) {
                return false;
            }
            if (system.operations().isSubtype(rs, rt)) {
                return true;
            }
            List<JExpressionInformation> results = lambda.resultExpressions();
            if (results.isEmpty()) {
                return false;
            }
            if (isFunctionalInterfaceType(rs) && isFunctionalInterfaceType(rt)) {
                return results.stream().allMatch(r -> moreSpecificType(system, rs, rt, r));
            }
            if (rs instanceof JPrimitiveType && !(rt instanceof JPrimitiveType)) {
                return results.stream().allMatch(r -> isStandalonePrimitive(system, r));
            }
            if (!(rs instanceof JPrimitiveType) && rt instanceof JPrimitiveType) {
                return results.stream().allMatch(r -> isStandaloneReferenceOrPoly(system, r));
            }
            return false;
        }
        if (isExactMethodReference(system, expr)) {
            // JLS §15.12.2.5 exact method reference
            if (rt.typeEquals(system.constants().voidType())) {
                return true;
            }
            if (system.operations().isSubtype(rs, rt)) {
                return true;
            }
            Boolean declarationReturnsPrimitive = exactMethodReferenceReturnsPrimitive(system, expr);
            if (declarationReturnsPrimitive == null) {
                return false;
            }
            return rs instanceof JPrimitiveType ? !(rt instanceof JPrimitiveType) && declarationReturnsPrimitive : rt instanceof JPrimitiveType && !declarationReturnsPrimitive;
        }
        return false;
    }

    private static boolean isExplicitlyTyped(JExpressionInformation.Lambda lambda) {
        // JLS §15.27.1 a lambda without parameters is explicitly typed
        return lambda.parameterCount() == 0 || lambda.explicitParameterTypes() != null && lambda.explicitParameterTypes().size() == lambda.parameterCount();
    }

    private static boolean isStandalonePrimitive(JTypeSystem system, JExpressionInformation expr) {
        if (!expr.isSimplyTyped()) {
            return false;
        }
        return expr.getSimpleType(system, Function.identity()).map(t -> t instanceof JPrimitiveType).orElse(false);
    }

    private static boolean isStandaloneReferenceOrPoly(JTypeSystem system, JExpressionInformation expr) {
        if (!expr.isSimplyTyped()) {
            return true;
        }
        return expr.getSimpleType(system, Function.identity()).map(t -> !(t instanceof JPrimitiveType)).orElse(false);
    }

    private static boolean generateFunctionalInterfaceConstraints(JTypeSystem system, JType si, JType ti, JExpressionInformation expr, List<Constraint> constraints) {
        JType capturedSi = captureConvert(system, si, constraints);
        Optional<JMethodReference> mtsOpt = JReduceLambdaExpression.findFunctionalMethod(capturedSi);
        Optional<JMethodReference> mtsPrimeOpt = JReduceLambdaExpression.findFunctionalMethod(si);
        Optional<JMethodReference> mttOpt = JReduceLambdaExpression.findFunctionalMethod(ti);
        if (mtsOpt.isEmpty() || mtsPrimeOpt.isEmpty() || mttOpt.isEmpty()) {
            return false;
        }

        JMethodReference mts = mtsOpt.get();
        JMethodReference mtsPrime = mtsPrimeOpt.get();
        JMethodReference mtt = mttOpt.get();

        if (mts.typeParameters().size() != mtt.typeParameters().size()) {
            return false;
        }
        if (mts.parameters().size() != mtt.parameters().size()) {
            return false;
        }
        if (mts.typeParameters().size() != mtsPrime.typeParameters().size()) {
            return false;
        }

        Map<JVarType, JType> thetaPrime = new LinkedHashMap<>();
        for (int j = 0; j < mtt.typeParameters().size(); j++) {
            thetaPrime.put(mtt.typeParameters().get(j), mts.typeParameters().get(j));
        }
        JVarTypeResolveVisitor thetaPrimeResolver = new JVarTypeResolveVisitor(thetaPrime);

        Predicate<JType> mentionsMts = t -> t.mentions(x -> x instanceof JVarType v && mts.typeParameters().contains(v));
        for (int j = 0; j < mtt.typeParameters().size(); j++) {
            JType boundA = mts.typeParameters().get(j).upperBound();
            JType boundB = thetaPrimeResolver.apply(mtt.typeParameters().get(j).upperBound());
            if (mentionsMts.test(boundA) && !boundB.isProperType()) {
                // JLS §18.5.4 a bound mentioning a type parameter of MTS against an improper bound
                return false;
            }
            constraints.add(new JTypeConstraints.Equal(boundA, boundB));
        }

        Map<JVarType, JType> thetaDoublePrime = new LinkedHashMap<>();
        for (int j = 0; j < mtt.typeParameters().size(); j++) {
            thetaDoublePrime.put(mtt.typeParameters().get(j), mtsPrime.typeParameters().get(j));
        }
        JVarTypeResolveVisitor thetaDoublePrimeResolver = new JVarTypeResolveVisitor(thetaDoublePrime);

        for (int j = 0; j < mts.parameters().size(); j++) {
            JType uj = mts.parameters().get(j);
            JType vj = thetaPrimeResolver.apply(mtt.parameters().get(j));
            if (mentionsMts.test(uj) && !vj.isProperType()) {
                // JLS §18.5.4 a formal parameter mentioning a type parameter of MTS against an improper type
                return false;
            }
            constraints.add(new JTypeConstraints.Subtype(vj, uj));

            JType ujPrime = mtsPrime.parameters().get(j);
            JType vjPrime = thetaDoublePrimeResolver.apply(mtt.parameters().get(j));
            constraints.add(new JTypeConstraints.Equal(vjPrime, ujPrime));
        }

        JType rs = mts.returnType();
        JType rt = thetaPrimeResolver.apply(mtt.returnType());
        if (mentionsMts.test(rs) && !rt.isProperType()) {
            // JLS §18.5.4 a return type mentioning a type parameter of MTS against an improper type
            return false;
        }
        return generateReturnTypeConstraints(system, rs, rt, expr, constraints);
    }

    private static boolean generateReturnTypeConstraints(JTypeSystem system, JType rs, JType rt, JExpressionInformation expr, List<Constraint> constraints) {
        if (rt.isProperType()) {
            return moreSpecificReturnType(system, rs, rt, expr);
        }
        if (expr instanceof JExpressionInformation.Conditional cond) {
            // JLS §18.5.4 a conditional expression applies recursively to both operands
            return generateReturnTypeConstraints(system, rs, rt, cond.whenTrue(), constraints) && generateReturnTypeConstraints(system, rs, rt, cond.whenFalse(), constraints);
        }
        if (expr instanceof JExpressionInformation.Lambda lambda && isExplicitlyTyped(lambda)) {
            if (rt.typeEquals(system.constants().voidType())) {
                return true;
            }
            if (rs.typeEquals(system.constants().voidType())) {
                return false;
            }
            List<JExpressionInformation> results = lambda.resultExpressions();
            if (!results.isEmpty() && isFunctionalInterfaceType(rs) && isFunctionalInterfaceType(rt) && !interfacesRelated(system, rs, rt)) {
                // JLS §18.5.4 RS more specific than RT for each result expression
                return results.stream().allMatch(r -> generateFunctionalInterfaceConstraints(system, rs, rt, r, constraints));
            }
            if (!results.isEmpty() && rs instanceof JPrimitiveType && !(rt instanceof JPrimitiveType) && results.stream().allMatch(r -> isStandalonePrimitive(system, r))) {
                return true;
            }
            if (!results.isEmpty() && !(rs instanceof JPrimitiveType) && rt instanceof JPrimitiveType && results.stream().allMatch(r -> isStandaloneReferenceOrPoly(system, r))) {
                return true;
            }
            // JLS §18.5.4 ‹RS <: RT›
            constraints.add(new JTypeConstraints.Subtype(rs, rt));
            return true;
        }
        if (isExactMethodReference(system, expr)) {
            if (rt.typeEquals(system.constants().voidType())) {
                return true;
            }
            Boolean declarationReturnsPrimitive = exactMethodReferenceReturnsPrimitive(system, expr);
            if (declarationReturnsPrimitive != null) {
                if (rs instanceof JPrimitiveType && !(rt instanceof JPrimitiveType) && declarationReturnsPrimitive) {
                    return true;
                }
                if (rt instanceof JPrimitiveType && !(rs instanceof JPrimitiveType) && !declarationReturnsPrimitive) {
                    return true;
                }
            }
            constraints.add(new JTypeConstraints.Subtype(rs, rt));
            return true;
        }
        // JLS §18.5.4 no other expression form makes Si more specific than Ti
        return false;
    }

    static JType captureConvert(JTypeSystem system, JType type, List<Constraint> constraints) {
        if (type instanceof JClassType ct && type.isProperType()) {
            // JLS §5.1.10
            return JTypeRelations.capture(ct);
        }
        return type;
    }

    public static boolean isExactMethodReference(JTypeSystem system, JExpressionInformation expr) {
        if (expr instanceof JExpressionInformation.InstantiationReference instRef && instRef.type() instanceof JArrayType) {
            return true;
        }
        return exactMethodReferenceDeclaration(system, expr).isPresent();
    }

    private static Boolean exactMethodReferenceReturnsPrimitive(JTypeSystem system, JExpressionInformation expr) {
        if (expr instanceof JExpressionInformation.InstantiationReference) {
            return false;
        }
        return exactMethodReferenceDeclaration(system, expr)
                .map(m -> m.returnType() instanceof JPrimitiveType)
                .orElse(null);
    }

    private static Optional<JMethodReference> exactMethodReferenceDeclaration(JTypeSystem system, JExpressionInformation expr) {
        if (expr instanceof JExpressionInformation.InvocationReference<?> methodRef) {
            JType site;
            if (methodRef.source() instanceof JClassReference classRef) {
                site = classRef;
            } else if (methodRef.source() instanceof JExpressionInformation sourceExpr) {
                List<JStandaloneType> types = JStandaloneTyping.resolve(sourceExpr, system);
                if (types.size() != 1) {
                    return Optional.empty();
                }
                site = types.getFirst().type();
            } else {
                return Optional.empty();
            }
            JClassReference declaring = methodRef instanceof JExpressionInformation.InvocationReference.Full<?> full ? full.declaring() : null;
            // JLS §15.13.1 the type to search has exactly one accessible member method with the name
            List<JMethodReference> candidates = JMethodLookup.members(site, methodRef.methodName()).stream()
                    .map(JMethodLookup.Member::method)
                    .filter(m -> declaring == null ? !m.hasModifier(AccessFlag.PRIVATE) || site instanceof JClassType sct && m.outerClass().typeEquals(sct.classReference())
                            : m.outerClass().accessFrom(declaring, null).canAccess(m.access()))
                    .toList();
            if (candidates.size() != 1) {
                return Optional.empty();
            }
            JMethodReference m = candidates.get(0);
            if (m.hasModifier(AccessFlag.VARARGS)) {
                return Optional.empty();
            }
            if (!m.typeParameters().isEmpty() && isEmpty(methodRef.explicitTypeArguments())) {
                return Optional.empty();
            }
            return Optional.of(m);
        }

        if (expr instanceof JExpressionInformation.InstantiationReference instRef) {
            if (instRef.type() instanceof JArrayType) {
                return Optional.empty();
            }
            if (!(instRef.type() instanceof JClassReference classRef)) {
                return Optional.empty();
            }

            List<JMethodReference> ctors = classRef.declaredMethods().stream()
                    .filter(m -> m.location().isConstructor() && !m.hasModifier(AccessFlag.PRIVATE))
                    .toList();
            if (ctors.size() != 1) {
                return Optional.empty();
            }

            JMethodReference ctor = ctors.get(0);
            if (ctor.hasModifier(AccessFlag.VARARGS)) {
                return Optional.empty();
            }
            if (!ctor.typeParameters().isEmpty() && isEmpty(instRef.explicitTypeArguments())) {
                return Optional.empty();
            }
            return Optional.of(ctor);
        }

        return Optional.empty();
    }

    private static boolean isEmpty(List<JArgumentType> typeArguments) {
        return typeArguments == null || typeArguments.isEmpty();
    }
}
