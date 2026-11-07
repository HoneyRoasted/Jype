package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JParameterizedClassType;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the generic-vs-generic/generic-vs-non-generic most-specific-method
 * comparison (JLS §15.12.2.5 / §18.5.4).
 *
 * JResolveMostSpecificMethod previously compared candidates using the types actually *inferred
 * for this call* on both sides (composing each branch's own {@code Infer} bindings with its
 * final metavariable instantiations). That is not what the spec says: per §18.5.4, m1's own
 * formal parameter types are never substituted - even if m1 is generic, its type parameters are
 * treated as ordinary (opaque) type variables, not resolved to whatever they happened to be
 * inferred as for this particular call. Only the *other* candidate (m2, and only when m2 is
 * itself generic) gets fresh inference variables substituted for its type parameters.
 *
 * Both expected outcomes below were verified against real {@code javac} (JDK 24), not assumed
 * from a reading of the spec text alone - see {@code pickNonGenericWinsOverGeneric} in
 * particular, which is a case where the previous (wrong) implementation's assumption about which
 * overload wins was actually backwards.
 */
public class JResolveMostSpecificMethodGenericTests {

    public static class Host {
        public static <T> String pick(T a, T b) {
            return "generic";
        }

        public static String pick(Object a, String b) {
            return "specific";
        }
    }

    public static class HostGenericWins {
        public static <T> String foo(T a, String b) {
            return "generic";
        }

        public static String foo(Object a, Object b) {
            return "specific";
        }
    }

    public static class HostGenericOnly {
        public static <T> String pick(T a, T b) {
            return "generic";
        }
    }

    public static class HostGenericVsGeneric {
        public static <T> String m(List<T> a) {
            return "list";
        }

        public static <T> String m(Collection<T> a) {
            return "collection";
        }
    }

    public interface Foo<T> {
        String make();
    }

    public interface Bar<T> {
        T make();
    }

    public static class HostGenericFunctionalInterfaceParameter {
        public static <T> String pick(Foo<T> f) {
            return "foo";
        }

        public static <T> String pick(Bar<T> f) {
            return "bar";
        }
    }

    public static class HostCapturedFunctionalInterfaceParameter {
        public static String pick(Foo<? extends Number> f) {
            return "foo";
        }

        public static <T> String pick(Bar<T> f) {
            return "bar";
        }
    }

    private static JExpressionInformation constant(Object value) {
        return new JExpressionInformation.Constant.Simple(value);
    }

    private static Set<String> survivingKinds(JClassReference host, String name, JExpressionInformation... args) {
        JTypeSystem ts = host.typeSystem();
        JClassReference string = ts.tryResolve(String.class);

        JExpressionInformation.MethodInvocation<?> invocation = new JExpressionInformation.MethodInvocation.Simple<>(
                host, null, host, name, List.of(args), Collections.emptyList());
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                invocation, JTypeConstraints.Compatible.Context.ASSIGNMENT, string);
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();

        return tree.branches().stream()
                .filter(b -> b.status().isTrue())
                .filter(b -> b.metadata().has(JTypeContext.ChosenMethod.class))
                .map(b -> b.metadata().first(JTypeContext.ChosenMethod.class).get().chosen().typeParameters().isEmpty() ? "specific" : "generic")
                .collect(Collectors.toSet());
    }

    @Test
    public void isolatedGenericOverloadResolvesCorrectlyOnItsOwn() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(HostGenericOnly.class);
        JClassReference string = ts.tryResolve(String.class);

        JExpressionInformation.MethodInvocation<?> invocation = new JExpressionInformation.MethodInvocation.Simple<>(
                host, null, host, "pick", List.of(constant("x"), constant("y")), Collections.emptyList());
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                invocation, JTypeConstraints.Compatible.Context.ASSIGNMENT, string);
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();

        assertTrue(tree.branches().stream().anyMatch(b -> b.status().isTrue()), "expected the generic pick(T,T) to resolve on its own with T=String");
    }

    /**
     * {@code <T> String pick(T a, T b)} vs {@code String pick(Object a, String b)}, called as
     * {@code pick("x", "y")}. Real javac (verified with JDK 24) picks the non-generic overload:
     * testing "is pick(T,T) more specific than pick(Object,String)" compares T (m1's own,
     * unsubstituted, bound-Object type variable) against String at the second parameter - T is
     * not a subtype of String, so that direction is false outright, independent of §18.5.4 (m2
     * isn't generic there). The reverse direction ("is pick(Object,String) more specific than
     * pick(T,T)") does invoke §18.5.4: a fresh alpha stands in for T, and both
     * {@code Object <: alpha} and {@code String <: alpha} are simultaneously satisfiable
     * (alpha=Object), so that direction is true - making the non-generic overload strictly more
     * specific.
     */
    @Test
    public void pickNonGenericWinsOverGeneric() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(Host.class);

        Set<String> survivingKinds = survivingKinds(host, "pick", constant("x"), constant("y"));

        assertEquals(1, survivingKinds.size(), "expected pick(\"x\", \"y\") to have exactly one surviving overload, not be ambiguous");
        assertTrue(survivingKinds.contains("specific"),
                "expected the non-generic pick(Object, String) overload to win, matching real javac");
    }

    /**
     * {@code <T> String foo(T a, String b)} vs {@code String foo(Object a, Object b)}, called as
     * {@code foo("x", "y")}. Real javac (verified with JDK 24) picks the generic overload here:
     * "is foo(T,String) more specific than foo(Object,Object)" only needs T <: Object and
     * String <: Object, both trivially true (m2 not generic, so no §18.5.4 needed on this side).
     * The reverse ("is foo(Object,Object) more specific than foo(T,String)") does invoke §18.5.4:
     * the second parameter's Ti (String) is a proper type (T doesn't appear there), so it's
     * checked directly - Object is not more specific than String, so that direction is false
     * outright - making the generic overload strictly more specific.
     */
    @Test
    public void fooGenericWinsOverNonGeneric() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(HostGenericWins.class);

        Set<String> survivingKinds = survivingKinds(host, "foo", constant("x"), constant("y"));

        assertEquals(1, survivingKinds.size(), "expected foo(\"x\", \"y\") to have exactly one surviving overload, not be ambiguous");
        assertTrue(survivingKinds.contains("generic"),
                "expected the generic foo(T, String) overload to win, matching real javac");
    }

    /**
     * {@code <T> String m(List<T> a)} vs {@code <T> String m(Collection<T> a)}, called with a
     * {@code List<String>} argument. Both overloads are generic, so this is the genuine
     * generic-vs-generic case that originally motivated this fix. Real javac (verified with JDK
     * 24) picks {@code List<T>}: "is m(List<T>) more specific than m(Collection<T>)" substitutes
     * a fresh alpha for the second method's T, generating {@code List<T> <: Collection<alpha>} -
     * satisfiable with alpha=T. The reverse direction substitutes a fresh alpha for the first
     * method's T instead, generating {@code Collection<T> <: List<alpha>}, which is not
     * satisfiable (a Collection is not a List), so only the first direction holds.
     */
    @Test
    public void listWinsOverCollectionForBothGenericOverloads() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(HostGenericVsGeneric.class);
        JClassReference listRef = ts.tryResolve(List.class);
        JClassReference collectionRef = ts.tryResolve(Collection.class);

        JParameterizedClassType listOfString = ts.typeFactory().newParameterizedClassType();
        listOfString.setClassReference(listRef);
        listOfString.setTypeArguments(List.of(ts.tryResolve(String.class)));

        JExpressionInformation.MethodInvocation<?> invocation = new JExpressionInformation.MethodInvocation.Simple<>(
                host, null, host, "m", List.of(JExpressionInformation.of(listOfString)), Collections.emptyList());
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                invocation, JTypeConstraints.Compatible.Context.ASSIGNMENT, ts.tryResolve(String.class));
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();

        Set<JClassReference> survivingDeclaringParamTypes = tree.branches().stream()
                .filter(b -> b.status().isTrue())
                .filter(b -> b.metadata().has(JTypeContext.ChosenMethod.class))
                .map(b -> ((JParameterizedClassType) b.metadata().first(JTypeContext.ChosenMethod.class).get().chosen().parameters().get(0)).classReference())
                .collect(Collectors.toSet());

        assertEquals(1, survivingDeclaringParamTypes.size(), "expected m(List<String>) to have exactly one surviving overload, not be ambiguous");
        assertTrue(survivingDeclaringParamTypes.contains(listRef), "expected the List<T> overload to win over Collection<T>, matching real javac");
        assertFalse(survivingDeclaringParamTypes.contains(collectionRef));
    }

    private static Set<JClassReference> survivingDeclaringParamTypes(JClassReference host, JExpressionInformation... args) {
        JTypeSystem ts = host.typeSystem();
        JClassReference string = ts.tryResolve(String.class);

        JExpressionInformation.MethodInvocation<?> invocation = new JExpressionInformation.MethodInvocation.Simple<>(
                host, null, host, "pick", List.of(args), Collections.emptyList());
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                invocation, JTypeConstraints.Compatible.Context.ASSIGNMENT, string);
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();

        return tree.branches().stream()
                .filter(b -> b.status().isTrue())
                .filter(b -> b.metadata().has(JTypeContext.ChosenMethod.class))
                .map(b -> ((JParameterizedClassType) b.metadata().first(JTypeContext.ChosenMethod.class).get().chosen().parameters().get(0)).classReference())
                .collect(Collectors.toSet());
    }

    /**
     * §18.5.4's own elaborate MTS/MTT bullet: {@code <T> String pick(Foo<T> f)} vs
     * {@code <T> String pick(Bar<T> f)} where {@code Foo<T> { String make(); }} and
     * {@code Bar<T> { T make(); }} are unrelated functional interfaces, called with
     * {@code pick(() -> "hi")}. Both candidates are generic *and* the type parameter being
     * compared sits directly in a functional-interface-typed parameter position - unlike
     * {@code unrelatedFunctionalInterfacesComparedByFunctionType} (which only covers the
     * non-generic-m2 §15.12.2.5 path), this exercises §18.5.4's own version of the same
     * function-type comparison, generating constraints into the nested inference solve rather
     * than deciding immediately. Real javac (verified with JDK 24) picks {@code Foo}: comparing
     * function types gives return types {@code String} (Foo) vs a fresh inference variable
     * (Bar's own T) - {@code String <: alpha} is satisfiable, but the reverse
     * ({@code alpha <: String} for Bar's own declared, unsubstituted T) is not, since T is treated
     * as an opaque type variable, not resolvable to String.
     */
    @Test
    public void fooWinsOverBarForGenericVsGenericWithFunctionalInterfaceParameter() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(HostGenericFunctionalInterfaceParameter.class);
        JClassReference fooRef = ts.tryResolve(Foo.class);
        JClassReference barRef = ts.tryResolve(Bar.class);

        JExpressionInformation.Lambda lambda = new JExpressionInformation.Lambda.Simple(constant("hi"), List.of(), 0, true);

        Set<JClassReference> surviving = survivingDeclaringParamTypes(host, lambda);

        assertEquals(1, surviving.size(), "expected pick(() -> \"hi\") to have exactly one surviving overload, not be ambiguous");
        assertTrue(surviving.contains(fooRef), "expected the Foo<T> overload to win over Bar<T>, matching real javac");
        assertFalse(surviving.contains(barRef));
    }

    /**
     * Same shape as {@code fooWinsOverBarForGenericVsGenericWithFunctionalInterfaceParameter}, but
     * {@code Foo}'s own parameter is wildcard-parameterized ({@code Foo<? extends Number>}) rather
     * than generic - exercising the §18.5.4 path's capture-conversion code (MTS = function type of
     * the *capture* of Si) through the full overload-resolution pipeline, end to end, rather than
     * only in isolation (see {@code JResolveMostSpecificMethodCaptureTests} for direct coverage of
     * the capture bound math itself). {@code Foo}'s SAM ({@code String make()}) doesn't actually
     * depend on its own type parameter, so the captured bound has no bearing on *which* overload
     * wins here - real javac (verified with JDK 24) still picks {@code Foo}, exactly as in the
     * fully-generic version above - but this exercises {@link JResolveMostSpecificMethod#captureConvert}
     * being invoked and its result flowing through {@code findFunctionalMethod}/the rest of the
     * comparison without regressing.
     */
    @Test
    public void fooStillWinsWhenSiHasWildcardTypeArgumentRequiringCapture() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(HostCapturedFunctionalInterfaceParameter.class);
        JClassReference fooRef = ts.tryResolve(Foo.class);
        JClassReference barRef = ts.tryResolve(Bar.class);

        JExpressionInformation.Lambda lambda = new JExpressionInformation.Lambda.Simple(constant("hi"), List.of(), 0, true);

        Set<JClassReference> surviving = survivingDeclaringParamTypes(host, lambda);

        assertEquals(1, surviving.size(), "expected pick(() -> \"hi\") to have exactly one surviving overload, not be ambiguous");
        assertTrue(surviving.contains(fooRef), "expected the Foo<? extends Number> overload to win over Bar<T>, matching real javac");
        assertFalse(surviving.contains(barRef));
    }
}
