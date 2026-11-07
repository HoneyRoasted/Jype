package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JResolveBoundsSecondAttemptTests {
    public static class Host {
        public static List<Integer> li;
        public static List<Number> ln;
        public static ArrayList<Integer> ali;
        public static List<? super Integer> lsup;

        public static List<? super Integer> superInteger;
        public static List<? super Number> superNumber;
        public static List<? extends Number> extendsNumber;
        public static Collection<? super Integer> collectionSuperInteger;

        public static <T> T pick(T a, T b) {
            return a;
        }

        public static <T> T id(T a) {
            return a;
        }

        public static <A extends Comparable<A>> A fbound() {
            throw new UnsupportedOperationException();
        }
    }

    private static final JTypeSystem TS = JTypeSystem.RUNTIME_REFLECTION;
    private static final JClassReference HOST = TS.tryResolve(Host.class);

    private static JType typeOf(String field) throws NoSuchFieldException {
        Field f = Host.class.getField(field);
        return TS.tryResolve(f.getGenericType());
    }

    private static JExpressionInformation field(String name) {
        return new JExpressionInformation.GetField.Simple<>(HOST, null, HOST, name);
    }

    private static JExpressionInformation call(String name, JExpressionInformation... args) {
        return new JExpressionInformation.MethodInvocation.Simple<>(HOST, null, HOST, name, List.of(args), List.of());
    }

    private static boolean solves(JExpressionInformation expr, JType target) {
        ConstraintTree tree = TS.operations().inferenceSolver().bind(new JTypeConstraints.ExpressionCompatible(expr, JTypeConstraints.Compatible.Context.ASSIGNMENT, target)).solve();
        return tree.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    @Test
    public void secondResolutionAttemptMatchesJavac() throws NoSuchFieldException {
        JExpressionInformation pickLiLn = call("pick", field("li"), field("ln"));
        assertEquals(true, solves(pickLiLn, typeOf("superInteger")), "List<? super Integer> x = pick(li, ln)");
        assertEquals(false, solves(call("id", pickLiLn), typeOf("superInteger")), "List<? super Integer> x = id(pick(li, ln))");
        assertEquals(true, solves(pickLiLn, typeOf("collectionSuperInteger")), "Collection<? super Integer> x = pick(li, ln)");
        assertEquals(true, solves(call("pick", field("li"), field("ali")), typeOf("superInteger")), "List<? super Integer> x = pick(li, ali)");
        assertEquals(false, solves(pickLiLn, typeOf("superNumber")), "List<? super Number> x = pick(li, ln)");
        assertEquals(true, solves(pickLiLn, typeOf("extendsNumber")), "List<? extends Number> x = pick(li, ln)");
        assertEquals(true, solves(call("pick", field("lsup"), field("li")), typeOf("superInteger")), "List<? super Integer> x = pick(lsup, li)");
        assertEquals(true, solves(call("fbound"), TS.tryResolve(Runnable.class)), "Runnable x = fbound()");
    }

    @Test
    public void nullReceiverCannotBeDereferenced() {
        JExpressionInformation clone = new JExpressionInformation.MethodInvocation.Simple<>(HOST, null, new JExpressionInformation.Constant.Simple(null), "clone", List.of(), List.of());
        assertEquals(false, solves(clone, TS.constants().object()), "Object x = (null).clone()");
    }
}
