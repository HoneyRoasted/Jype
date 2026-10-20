package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class JResolveMostSpecificMethodTests {

    public static class Overloads {
        public static String strictExact(int a, int b) { return "int,int"; }
        public static String strictExact(long a, long b) { return "long,long"; }
        public static String strictExact(Integer a, Integer b) { return "Integer,Integer"; }
    }

    public static class VarargOnly {
        public static String call(int... a) { return "varargs"; }
    }

    public interface Foo {
    }

    public interface Bar {
    }

    public static class FooBarImpl implements Foo, Bar {
    }

    public static class AmbiguousHost {
        public static String pick(Foo f) { return "Foo"; }
        public static String pick(Bar b) { return "Bar"; }
    }

    public static class CrossedHost {
        public static String amb(String s, Object o) { return "String,Object"; }
        public static String amb(Object o, String s) { return "Object,String"; }
    }

    public static class Ctors {
        public Ctors(int a) {}
        public Ctors(long a) {}
    }

    public static class VarargCtors {
        public VarargCtors(int a) {}
        public VarargCtors(int... a) {}
    }

    public interface StringMaker {
        String make();
    }

    public interface CharSequenceMaker {
        CharSequence make();
    }

    public static class UnrelatedFunctionalInterfaceHost {
        public static String pick(StringMaker f) { return "string"; }
        public static String pick(CharSequenceMaker f) { return "charseq"; }
    }

    public interface IntSupplier2 {
        int get();
    }

    public interface ObjSupplier2 {
        Object get();
    }

    public interface Outer1 {
        IntSupplier2 make();
    }

    public interface Outer2 {
        ObjSupplier2 make();
    }

    public static class ExactMethodReferenceHost {
        public static String pick(Outer1 f) { return "outer1"; }
        public static String pick(Outer2 f) { return "outer2"; }

        public static int five() { return 5; }
    }

    private static JExpressionInformation constant(Object value) {
        return new JExpressionInformation.Constant.Simple(value);
    }

    private static ConstraintTree solveInvocation(JTypeSystem ts, JClassReference declaring, String name, JType targetType, JExpressionInformation... args) {
        JExpressionInformation.MethodInvocation<?> invocation = new JExpressionInformation.MethodInvocation.Simple<>(
                declaring, null, declaring, name, List.of(args), Collections.emptyList());
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                invocation, JTypeConstraints.Compatible.Context.ASSIGNMENT, targetType);
        return ts.operations().inferenceSolver().bind(constraint).solve();
    }

    private static ConstraintTree solveInstantiation(JTypeSystem ts, JClassReference declaring, JClassReference target, JExpressionInformation... args) {
        JExpressionInformation.Instantiation inst = new JExpressionInformation.Instantiation.Simple(
                declaring, null, target, List.of(args), Collections.emptyList());
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                inst, JTypeConstraints.Compatible.Context.ASSIGNMENT, target);
        return ts.operations().inferenceSolver().bind(constraint).solve();
    }

    private static List<ConstraintBranch> chosenMethodBranches(ConstraintTree tree) {
        return tree.branches().stream()
                .filter(b -> b.status().isTrue())
                .filter(b -> b.metadata().has(JTypeContext.ChosenMethod.class))
                .toList();
    }

    /**
     * A single winning candidate can legitimately survive as both a strict- and a
     * loose-invocation branch (strict implies loose), so the JLS-level invariant to check
     * is "exactly one distinct method wins", not "exactly one branch survives".
     */
    private static Set<JMethodReference> survivingMethods(ConstraintTree tree) {
        return chosenMethodBranches(tree).stream()
                .map(b -> b.metadata().first(JTypeContext.ChosenMethod.class).get().chosen())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    @Test
    public void exactStrictOverloadIsTheOnlySurvivingBranch() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference overloads = ts.tryResolve(Overloads.class);

        ConstraintTree tree = solveInvocation(ts, overloads, "strictExact", ts.tryResolve(String.class), constant(1), constant(2));

        assertEquals(1, survivingMethods(tree).size(), "expected exactly one surviving overload");

        List<ConstraintBranch> survivors = chosenMethodBranches(tree);
        JTypeContext.ChosenMethod chosen = survivors.get(0).metadata().first(JTypeContext.ChosenMethod.class).get();
        assertEquals(JTypeConstraints.Compatible.Context.STRICT_INVOCATION, chosen.context());
        assertTrue(chosen.chosen().parameters().get(0).typeEquals(ts.constants().intType()));
        assertTrue(chosen.chosen().parameters().get(1).typeEquals(ts.constants().intType()));
    }

    @Test
    public void variableArityOnlyUsedWhenNoFixedArityOverloadApplies() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference varargOnly = ts.tryResolve(VarargOnly.class);

        ConstraintTree tree = solveInvocation(ts, varargOnly, "call", ts.tryResolve(String.class), constant(1), constant(2), constant(3));

        assertEquals(1, survivingMethods(tree).size(), "expected exactly one surviving (variable arity) overload");

        List<ConstraintBranch> survivors = chosenMethodBranches(tree);
        JTypeContext.ChosenMethod chosen = survivors.get(0).metadata().first(JTypeContext.ChosenMethod.class).get();
        assertTrue(chosen.vararg());
    }

    @Test
    public void unrelatedApplicableOverloadsLeaveNoSurvivor() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference ambiguousHost = ts.tryResolve(AmbiguousHost.class);
        JType fooBarImpl = ts.tryResolve(FooBarImpl.class);

        ConstraintTree tree = solveInvocation(ts, ambiguousHost, "pick", ts.tryResolve(String.class), JExpressionInformation.of(fooBarImpl));

        List<ConstraintBranch> survivors = chosenMethodBranches(tree);
        assertTrue(survivors.isEmpty(), "expected a genuinely ambiguous call to have no surviving overload");
    }

    /**
     * Regression test for a ConstraintBranch identity collision: crossed overloads called with
     * same-typed arguments used to reduce to identical constraint sets after
     * JReduceSimplyTypedExpression erased each argument's identity down to just its type,
     * causing one candidate's branch to be silently merged away and the genuine JLS ambiguity
     * to go undetected. The Constraint.identity(...) marker added to each candidate branch
     * prevents that collision.
     */
    @Test
    public void crossedSameTypedOverloadsLeaveNoSurvivor() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference crossedHost = ts.tryResolve(CrossedHost.class);

        ConstraintTree tree = solveInvocation(ts, crossedHost, "amb", ts.tryResolve(String.class), constant("a"), constant("b"));

        assertTrue(survivingMethods(tree).isEmpty(), "expected a genuinely ambiguous crossed-overload call to have no surviving overload");
    }

    @Test
    public void constructorOverloadResolutionPicksMostSpecific() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference ctors = ts.tryResolve(Ctors.class);

        ConstraintTree tree = solveInstantiation(ts, ctors, ctors, constant(1));

        assertEquals(1, survivingMethods(tree).size(), "expected exactly one surviving constructor overload");

        List<ConstraintBranch> survivors = chosenMethodBranches(tree);
        JTypeContext.ChosenMethod chosen = survivors.get(0).metadata().first(JTypeContext.ChosenMethod.class).get();
        assertTrue(chosen.chosen().parameters().get(0).typeEquals(ts.constants().intType()));
    }

    /**
     * Regression test for JReduceInstantiation.createVarargConstruct hardcoding vararg=false in
     * the ChosenMethod it attaches. With that bug, a vararg constructor branch was
     * mis-classified as fixed-arity (wrong phase rank) and its effective parameters were never
     * spread to the vararg component type, causing a fixed-arity constructor that should win
     * outright (JLS phase 1 beats phase 3) to instead be reported as falsely ambiguous against
     * the vararg constructor.
     */
    @Test
    public void fixedArityConstructorBeatsVarargConstructor() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference varargCtors = ts.tryResolve(VarargCtors.class);

        ConstraintTree tree = solveInstantiation(ts, varargCtors, varargCtors, constant(1));

        assertEquals(1, survivingMethods(tree).size(), "expected the fixed-arity constructor to win outright, not be ambiguous with the vararg one");

        List<ConstraintBranch> survivors = chosenMethodBranches(tree);
        JTypeContext.ChosenMethod chosen = survivors.get(0).metadata().first(JTypeContext.ChosenMethod.class).get();
        assertFalse(chosen.vararg(), "expected the fixed-arity constructor to be chosen over the vararg one");
    }

    /**
     * Regression test for JLS §15.12.2.5's functional-interface-specific "more specific" rule:
     * {@code pick(StringMaker)} vs {@code pick(CharSequenceMaker)}, called with a lambda
     * ({@code () -> "hi"}). Neither {@code StringMaker} nor {@code CharSequenceMaker} is a
     * super/subinterface of the other, so a plain {@code S <: T} check between the two interface
     * types themselves is false in both directions - the two must instead be compared by their
     * function types (matching arity, formal parameters, and return type: {@code String} vs
     * {@code CharSequence}). Real javac (verified with JDK 24) picks {@code StringMaker}, since
     * once the two are compared by return type, {@code String <: CharSequence}.
     */
    @Test
    public void unrelatedFunctionalInterfacesComparedByFunctionType() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(UnrelatedFunctionalInterfaceHost.class);

        JExpressionInformation.Lambda lambda = new JExpressionInformation.Lambda.Simple(constant("hi"), List.of(), 0, true);
        ConstraintTree tree = solveInvocation(ts, host, "pick", ts.tryResolve(String.class), lambda);

        assertEquals(1, survivingMethods(tree).size(), "expected pick(() -> \"hi\") to have exactly one surviving overload, not be ambiguous");

        List<ConstraintBranch> survivors = chosenMethodBranches(tree);
        JTypeContext.ChosenMethod chosen = survivors.get(0).metadata().first(JTypeContext.ChosenMethod.class).get();
        assertTrue(chosen.chosen().parameters().get(0).simpleName().contains("StringMaker"),
                "expected the StringMaker overload to win, matching real javac (String is more specific than CharSequence)");
    }

    /**
     * Regression test for JLS §15.12.2.5's exact-method-reference return-type bullet:
     * {@code pick(Outer1)} vs {@code pick(Outer2)}, where {@code Outer1.make()} returns
     * {@code IntSupplier2} ({@code int get()}) and {@code Outer2.make()} returns
     * {@code ObjSupplier2} ({@code Object get()}), called with
     * {@code pick(() -> ExactMethodReferenceHost::five)}. The method reference is wrapped in a
     * lambda rather than passed directly - passed directly, matching {@code five()}'s return type
     * (int) against a primitive-returning SAM succeeds by *strict* invocation while matching it
     * against a reference-returning SAM needs boxing and only succeeds by *loose* invocation, so
     * phase-based overload resolution (strict beats loose, JLS §15.12.2) would settle the call
     * before the "most specific method" comparison this bullet lives in is ever reached - wrapping
     * in a lambda avoids that, since a lambda's own phase-applicability doesn't depend on its
     * body's boxing behavior, letting both {@code Outer1}/{@code Outer2} survive to the same phase
     * and forcing the comparison to recurse into the lambda's result expression (the method
     * reference) via §15.12.2.5's explicitly-typed-lambda bullet
     * ("RS and RT are functional interface types ... RS is more specific than RT for each result
     * expression"). Real javac (verified with JDK 24) picks {@code Outer1}: once recursed in,
     * {@code IntSupplier2}/{@code ObjSupplier2} are themselves unrelated functional interfaces
     * compared by function type, and {@code five} is an exact, non-generic, non-variable-arity,
     * unambiguous method reference, so its compile-time declaration's primitive return type
     * applies directly rather than falling back to a plain {@code int <: Object} check (false,
     * since JLS subtyping doesn't include boxing) - without this bullet, neither
     * {@code IntSupplier2} nor {@code ObjSupplier2} would be more specific than the other at that
     * inner level, making the call wrongly ambiguous.
     */
    @Test
    public void exactMethodReferenceWithPrimitiveReturnPicksPrimitiveMaker() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(ExactMethodReferenceHost.class);

        JExpressionInformation.InvocationReference<JClassReference> ref =
                new JExpressionInformation.InvocationReference.Simple<>(host, "five", Collections.emptyList());
        JExpressionInformation.Lambda lambda = new JExpressionInformation.Lambda.Simple(ref, List.of(), 0, true);
        ConstraintTree tree = solveInvocation(ts, host, "pick", ts.tryResolve(String.class), lambda);

        assertEquals(1, survivingMethods(tree).size(), "expected pick(() -> ExactMethodReferenceHost::five) to have exactly one surviving overload, not be ambiguous");

        List<ConstraintBranch> survivors = chosenMethodBranches(tree);
        JTypeContext.ChosenMethod chosen = survivors.get(0).metadata().first(JTypeContext.ChosenMethod.class).get();
        assertTrue(chosen.chosen().parameters().get(0).simpleName().contains("Outer1"),
                "expected the Outer1 overload to win, matching real javac (the compile-time declaration's primitive return type breaks the tie one level down)");
    }

}
