package honeyroasted.jype.system.visitor;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.JCapturedWildcard;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JProjectionTests {
    public static class Rec<T extends Rec<T>> {
    }

    public static List<? extends Number> lnum;
    public static List<? super Integer> lsup;
    public static Comparable<?> cq;
    public static Rec<?> rq;
    public static Rec<? extends Rec<?>> rqExpected;

    private static final JTypeSystem TS = JTypeSystem.RUNTIME_REFLECTION;
    private static final Predicate<JType> CAPTURED = t -> t instanceof JVarType v && v.metadata().has(JCapturedWildcard.class);

    private static JType field(String name) throws NoSuchFieldException {
        return TS.tryResolve(JProjectionTests.class.getField(name).getGenericType());
    }

    private static JType varType(String name) throws NoSuchFieldException {
        return JTypeRelations.capture((JClassType) field(name)).upwardsProjection(CAPTURED);
    }

    private static void assertEquivalent(JType expected, JType actual, String message) {
        assertTrue(JTypeRelations.isSubtype(actual, expected) && JTypeRelations.isSubtype(expected, actual), message + ": " + actual.simpleName());
    }

    @Test
    public void upwardProjectionMatchesJavacVarTypes() throws NoSuchFieldException {
        assertEquivalent(field("lnum"), varType("lnum"), "var x = lnum");
        assertEquivalent(field("lsup"), varType("lsup"), "var x = lsup");
        assertEquivalent(field("cq"), varType("cq"), "var x = cq");
        assertEquivalent(field("rqExpected"), varType("rq"), "var x = rq");
        assertEquals(false, varType("rq").mentions(CAPTURED), "the projection mentions no captured variable");
    }

    @Test
    public void downwardProjectionIsUndefinedForNonWildcardArguments() throws NoSuchFieldException {
        JClassType captured = JTypeRelations.capture((JClassType) field("lnum"));
        assertEquals(true, captured.downwardsProjection(CAPTURED).isEmpty(), "List<CAP> has no downward projection");
        JClassType lsupCaptured = JTypeRelations.capture((JClassType) field("lsup"));
        JType lowerOfCap = lsupCaptured.typeArguments().getFirst();
        assertEquals(true, lowerOfCap.downwardsProjection(CAPTURED).map(t -> t.typeEquals(TS.tryResolve(Integer.class))).orElse(false), "CAP of ? super Integer projects down to Integer");
    }
}
