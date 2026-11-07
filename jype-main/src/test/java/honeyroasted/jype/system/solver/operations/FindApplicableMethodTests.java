package honeyroasted.jype.system.solver.operations;

import honeyroasted.almonds.SimpleName;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JParameterizedMethodType;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AccessFlag;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FindApplicableMethodTests {

    public static class Overloads {
        public static String strictExact(int a, int b) { return "int,int"; }
        public static String strictExact(long a, long b) { return "long,long"; }
        public static String strictExact(Integer a, Integer b) { return "Integer,Integer"; }
        public static String strictExact(int... a) { return "varargs"; }

        public static String box(Integer a) { return "Integer"; }

        public static <T> T identity(T t) { return t; }
        public static <T extends Number> T firstNumber(T a, T b) { return a; }

        public String instanceMethod(String s) { return s; }

        public Overloads self() { return this; }

        public static void doesNotAcceptString(int a) {}
    }

    public static class ArityPriority {
        public static String call(int a) { return "fixed"; }
        public static String call(int... a) { return "vararg"; }
    }

    public static class Specificity {
        public static String pick(Object o) { return "Object"; }
        public static String pick(String s) { return "String"; }
    }

    public static class AmbiguousHost {
        public static String amb(String s, Object o) { return "String,Object"; }
        public static String amb(Object o, String s) { return "Object,String"; }
    }

    public static class Base {
        public String overridden() { return "base"; }
        public String baseOnly() { return "baseOnly"; }
    }

    public static class Derived extends Base {
        @Override
        public String overridden() { return "derived"; }
    }

    public static class Owner {
        public String publicMethod(String s) { return "public"; }
        String packagePrivateMethod(String s) { return "package"; }
        private String privateMethod(String s) { return "private"; }
    }

    public static class Caller {
    }

    public interface DiamondA {
        String diamond(String s);
    }

    public interface DiamondB {
        String diamond(String s);
    }

    public static abstract class DiamondImpl implements DiamondA, DiamondB {
    }

    private static <T extends SimpleName> JExpressionInformation.MethodInvocation<T> invoke(JClassReference declaring, T source, String name, List<JArgumentType> explicitTypeArgs, JExpressionInformation... args) {
        return new JExpressionInformation.MethodInvocation.Simple<>(declaring, null, source, name, List.of(args), explicitTypeArgs);
    }

    private static <T extends SimpleName> JExpressionInformation.MethodInvocation<T> invoke(JClassReference declaring, T source, String name, JExpressionInformation... args) {
        return invoke(declaring, source, name, Collections.emptyList(), args);
    }

    private static JExpressionInformation arg(JType type) {
        return JExpressionInformation.of(type);
    }

    @Test
    public void strictPhasePicksExactMatchOverWiderAndVarargOverloads() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference overloads = ts.tryResolve(Overloads.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(overloads, overloads, "strictExact", arg(ts.constants().intType()), arg(ts.constants().intType())));

        assertNotNull(result);
        assertFalse(result.hasModifier(AccessFlag.VARARGS));
        assertEquals(2, result.parameters().size());
        assertTrue(result.parameters().get(0).typeEquals(ts.constants().intType()));
        assertTrue(result.parameters().get(1).typeEquals(ts.constants().intType()));
    }

    @Test
    public void loosePhaseAppliesBoxingWhenStrictPhaseHasNoMatch() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference overloads = ts.tryResolve(Overloads.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(overloads, overloads, "box", arg(ts.constants().intType())));

        assertNotNull(result);
        assertTrue(result.parameters().get(0).typeEquals(ts.tryResolve(Integer.class)));
    }

    @Test
    public void variableArityPhaseAppliesWhenNoFixedArityMatchExists() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference overloads = ts.tryResolve(Overloads.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(overloads, overloads, "strictExact",
                        arg(ts.constants().intType()), arg(ts.constants().intType()), arg(ts.constants().intType())));

        assertNotNull(result);
        assertTrue(result.hasModifier(AccessFlag.VARARGS));
        assertEquals(1, result.parameters().size());
    }

    @Test
    public void fixedArityIsPreferredOverVariableArityWhenBothCouldApply() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference arityPriority = ts.tryResolve(ArityPriority.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(arityPriority, arityPriority, "call", arg(ts.constants().intType())));

        assertNotNull(result);
        assertFalse(result.hasModifier(AccessFlag.VARARGS));
    }

    @Test
    public void mostSpecificMethodIsChosenAmongApplicableOverloads() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference specificity = ts.tryResolve(Specificity.class);
        JType stringType = ts.tryResolve(String.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(specificity, specificity, "pick", arg(stringType)));

        assertNotNull(result);
        assertTrue(result.parameters().get(0).typeEquals(stringType));
    }

    @Test
    public void crossedOverloadsWithNoMostSpecificMethodAreAmbiguous() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference ambiguousHost = ts.tryResolve(AmbiguousHost.class);
        JType stringType = ts.tryResolve(String.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(ambiguousHost, ambiguousHost, "amb", arg(stringType), arg(stringType)));

        assertNull(result);
    }

    @Test
    public void diamondAbstractMethodFromUnrelatedInterfacesIsNotAmbiguous() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference diamondImpl = ts.tryResolve(DiamondImpl.class);
        JType stringType = ts.tryResolve(String.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(diamondImpl, JExpressionInformation.of(diamondImpl), "diamond", arg(stringType)));

        assertNotNull(result, "inheriting the same abstract method signature from two unrelated interfaces should not be ambiguous");
    }

    @Test
    public void genericStaticMethodResultCanBeCopiedWithoutOuterType() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference overloads = ts.tryResolve(Overloads.class);
        JType stringType = ts.tryResolve(String.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(overloads, overloads, "identity", arg(stringType)));

        assertNotNull(result);
        JParameterizedMethodType copy = result.copy();
        assertTrue(copy.typeArguments().get(0).typeEquals(stringType));
    }

    @Test
    public void genericMethodInfersTypeArgumentFromArgument() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference overloads = ts.tryResolve(Overloads.class);
        JType stringType = ts.tryResolve(String.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(overloads, overloads, "identity", arg(stringType)));

        assertNotNull(result);
        assertTrue(result.hasTypeArguments());
        assertTrue(result.typeArguments().get(0).typeEquals(stringType));
    }

    @Test
    public void genericMethodHonorsExplicitTypeArgument() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference overloads = ts.tryResolve(Overloads.class);
        JType stringType = ts.tryResolve(String.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(overloads, overloads, "identity", List.of((JArgumentType) stringType), arg(stringType)));

        assertNotNull(result);
        assertTrue(result.typeArguments().get(0).typeEquals(stringType));
    }

    @Test
    public void explicitTypeArgumentArityMismatchExcludesCandidate() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference overloads = ts.tryResolve(Overloads.class);
        JType stringType = ts.tryResolve(String.class);
        JType integerType = ts.tryResolve(Integer.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(overloads, overloads, "identity", List.of((JArgumentType) stringType, (JArgumentType) integerType), arg(stringType)));

        assertNull(result);
    }

    @Test
    public void boundedGenericMethodInfersTypeArgumentWithinBound() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference overloads = ts.tryResolve(Overloads.class);
        JType integerType = ts.tryResolve(Integer.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(overloads, overloads, "firstNumber", arg(integerType), arg(integerType)));

        assertNotNull(result);
        assertTrue(result.typeArguments().get(0).typeEquals(integerType));
    }

    @Test
    public void instanceMethodResolvesOnKnownReceiverType() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference overloads = ts.tryResolve(Overloads.class);
        JType stringType = ts.tryResolve(String.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(overloads, JExpressionInformation.of(overloads), "instanceMethod", arg(stringType)));

        assertNotNull(result);
    }

    @Test
    public void chainedMethodCallResolvesReceiverTypeFromInnerInvocation() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference overloads = ts.tryResolve(Overloads.class);
        JType stringType = ts.tryResolve(String.class);

        JExpressionInformation.MethodInvocation<?> chain = invoke(overloads, JExpressionInformation.of(overloads), "self");
        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(overloads, chain, "instanceMethod", arg(stringType)));

        assertNotNull(result);
    }

    @Test
    public void overriddenMethodResolvesToMostDerivedDeclaration() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference derived = ts.tryResolve(Derived.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(derived, JExpressionInformation.of(derived), "overridden"));

        assertNotNull(result);
        assertTrue(result.outerClass().typeEquals(derived));
    }

    @Test
    public void inheritedMethodIsReachableThroughSubclassReceiver() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference derived = ts.tryResolve(Derived.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(derived, JExpressionInformation.of(derived), "baseOnly"));

        assertNotNull(result);
    }

    @Test
    public void privateMethodIsNotAccessibleFromAnotherTopLevelClass() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference owner = ts.tryResolve(honeyroasted.jype.system.solver.fixtures.privateaccess.PrivateOwner.class);
        JClassReference caller = ts.tryResolve(Caller.class);
        JType stringType = ts.tryResolve(String.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(caller, JExpressionInformation.of(owner), "privateMethod", arg(stringType)));

        assertNull(result);
    }

    @Test
    public void privateMethodIsAccessibleFromNestmate() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference owner = ts.tryResolve(Owner.class);
        JClassReference caller = ts.tryResolve(Caller.class);
        JType stringType = ts.tryResolve(String.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(caller, JExpressionInformation.of(owner), "privateMethod", arg(stringType)));

        assertNotNull(result, "JLS 6.6.1: private members are accessible within the enclosing top level class");
    }

    @Test
    public void privateMethodIsAccessibleFromDeclaringClassItself() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference owner = ts.tryResolve(Owner.class);
        JType stringType = ts.tryResolve(String.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(owner, JExpressionInformation.of(owner), "privateMethod", arg(stringType)));

        assertNotNull(result);
    }

    @Test
    public void packagePrivateMethodIsAccessibleFromSamePackage() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference owner = ts.tryResolve(Owner.class);
        JClassReference caller = ts.tryResolve(Caller.class);
        JType stringType = ts.tryResolve(String.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(caller, JExpressionInformation.of(owner), "packagePrivateMethod", arg(stringType)));

        assertNotNull(result);
    }

    @Test
    public void unknownMethodNameReturnsNull() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference overloads = ts.tryResolve(Overloads.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(overloads, overloads, "doesNotExist", arg(ts.constants().intType())));

        assertNull(result);
    }

    @Test
    public void incompatibleArgumentTypeReturnsNull() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference overloads = ts.tryResolve(Overloads.class);
        JType stringType = ts.tryResolve(String.class);

        JParameterizedMethodType result = ts.operations().findApplicableMethod(
                invoke(overloads, overloads, "doesNotAcceptString", arg(stringType)));

        assertNull(result);
    }

}
