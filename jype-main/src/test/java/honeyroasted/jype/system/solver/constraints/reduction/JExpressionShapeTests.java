package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.JStandaloneType;
import honeyroasted.jype.system.solver.JStandaloneTyping;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JExpressionShapeTests {
    public static class Holder {
        public sealed interface Sealed permits Leaf {
        }

        public static final class Leaf implements Sealed {
        }

        public static class Open {
        }

        public interface Iface {
        }

        public static final class Closed {
        }

        public static int[] ints;
        public static String[] strs;
        public static Object obj;
        public static int i;
        public static short s;
        public static String str;
        public static boolean flag;
        public static Integer boxed;
        public static final int fin = 3;
        public static long l;
        public static double d;

        public void inst() {
        }

        public static void staticContext() {
        }

        public void instanceContext() {
        }
    }

    private static final JTypeSystem TS = JTypeSystem.RUNTIME_REFLECTION;
    private static final JClassReference HOLDER = TS.tryResolve(Holder.class);

    private static boolean solves(JExpressionInformation expr, JType target) {
        ConstraintTree tree = TS.operations().inferenceSolver().bind(new JTypeConstraints.ExpressionCompatible(expr, JTypeConstraints.Compatible.Context.ASSIGNMENT, target)).solve();
        return tree.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    private static JExpressionInformation c(Object v) {
        return new JExpressionInformation.Constant.Simple(v);
    }

    private static JExpressionInformation field(String name) {
        return new JExpressionInformation.GetField.Simple<>(HOLDER, null, HOLDER, name);
    }

    private static JExpressionInformation index(String array, JExpressionInformation idx) {
        return new JExpressionInformation.ArrayAccess(field(array), idx);
    }

    private static JExpressionInformation assign(JExpressionInformation variable, JExpressionInformation.BinaryOperator.Kind kind, JExpressionInformation value) {
        return new JExpressionInformation.Assignment(variable, kind, value);
    }

    private static JExpressionInformation yielding(JExpressionInformation... results) {
        return new JExpressionInformation.Switch(field("i"), List.of(results), false);
    }

    private static JExpressionInformation anonymous(Class<?> type, List<JExpressionInformation> args) {
        return new JExpressionInformation.Instantiation.Anonymous(HOLDER, null, TS.tryResolve(type), args, List.of(), List.of(), null);
    }

    private static JExpressionInformation cast(JExpressionInformation operand, Class<?> type) {
        return new JExpressionInformation.Cast.Simple(operand, TS.tryResolve(type));
    }

    private static JMethodReference method(String name) throws NoSuchMethodException {
        return TS.tryResolve(Holder.class.getDeclaredMethod(name));
    }

    @Test
    public void arrayAccessFollowsJavac() {
        var k = TS.constants();
        assertEquals(true, solves(index("ints", c(0)), k.intType()), "int x = ints[0]");
        assertEquals(false, solves(index("ints", c("s")), k.intType()), "int x = ints[\"s\"]");
        assertEquals(false, solves(index("ints", field("l")), k.intType()), "int x = ints[l]");
        assertEquals(false, solves(index("obj", c(0)), k.intType()), "int x = obj[0]");
        assertEquals(true, solves(index("ints", field("s")), TS.tryResolve(Integer.class)), "Integer x = ints[s]");
        assertEquals(true, solves(index("strs", c(0)), TS.tryResolve(String.class)), "String x = strs[0]");
    }

    @Test
    public void assignmentFollowsJavac() {
        var k = TS.constants();
        var add = JExpressionInformation.BinaryOperator.Kind.ADD;
        assertEquals(true, solves(assign(field("i"), add, c(2.5)), k.intType()), "int x = (i += 2.5)");
        assertEquals(true, solves(assign(field("str"), add, c(1)), TS.tryResolve(String.class)), "String x = (str += 1)");
        assertEquals(false, solves(assign(field("obj"), add, c(1)), k.object()), "Object x = (obj += 1)");
        assertEquals(false, solves(assign(field("fin"), null, c(3)), k.intType()), "int x = (fin = 3)");
        assertEquals(false, solves(assign(new JExpressionInformation.GetField.Simple<>(HOLDER, null, field("ints"), "length"), null, c(3)), k.intType()), "int x = (ints.length = 3)");
        assertEquals(true, solves(assign(field("s"), add, c(1)), k.shortType()), "short x = (s += 1)");
        assertEquals(false, solves(assign(field("s"), null, c(70000)), k.intType()), "int x = (s = 70000)");
        assertEquals(true, solves(assign(index("ints", c(0)), JExpressionInformation.BinaryOperator.Kind.SHL, field("l")), k.longType()), "long x = (ints[0] <<= l)");
        assertEquals(true, solves(assign(field("flag"), JExpressionInformation.BinaryOperator.Kind.AND, c(true)), k.booleanType()), "boolean x = (flag &= true)");
        assertEquals(false, solves(assign(field("flag"), add, c(true)), k.booleanType()), "boolean x = (flag += true)");
    }

    @Test
    public void incrementDecrementFollowsJavac() {
        var k = TS.constants();
        assertEquals(true, solves(new JExpressionInformation.IncrementDecrement(field("boxed"), true, false), k.intType()), "int x = (boxed++)");
        assertEquals(false, solves(new JExpressionInformation.IncrementDecrement(field("flag"), true, false), k.booleanType()), "boolean x = (flag++)");
        assertEquals(true, solves(new JExpressionInformation.IncrementDecrement(field("d"), false, true), k.doubleType()), "double x = (--d)");
        assertEquals(false, solves(new JExpressionInformation.IncrementDecrement(field("fin"), true, false), k.intType()), "int x = (fin++)");
    }

    @Test
    public void switchExpressionsFollowJavac() {
        var k = TS.constants();
        assertEquals(false, solves(yielding(c(1), c(2L)), k.intType()), "int x = switch (i) { ... yield 1; yield 2L; }");
        assertEquals(true, solves(yielding(c(1), c(2L)), k.longType()), "long x = switch (i) { ... yield 1; yield 2L; }");
        JClassReference supplier = TS.tryResolve(Supplier.class);
        JExpressionInformation lambda = new JExpressionInformation.Lambda.Simple(List.of(), null, new JExpressionInformation.LambdaBody.Expression(c("s")));
        assertEquals(true, solves(yielding(lambda), supplier.parameterized(List.of((JArgumentType) TS.tryResolve(String.class)))), "Supplier<String> x = switch (i) { default -> () -> \"s\"; }");
        assertEquals(false, solves(new JExpressionInformation.Switch(field("l"), List.of(c(1)), false), k.object()), "Object x = switch (l) { default -> 1; }");
        assertEquals(true, solves(new JExpressionInformation.Switch(field("str"), List.of(c(1)), true), k.object()), "Object x = switch (str) { case Object o -> 1; }");
        assertEquals(true, solves(yielding(c(1), c(2)), k.byteType()), "byte x = switch (i) { ... yield 1; yield 2; }");
        List<JStandaloneType> standalone = JStandaloneTyping.resolve(yielding(c(1), new JExpressionInformation.Cast.Simple(c(2), k.shortType())), TS);
        assertEquals(List.of("short"), standalone.stream().map(t -> t.type().simpleName()).toList(), "var x = switch (i) { ... yield 1; yield (short) 2; }");
    }

    @Test
    public void anonymousClassesFollowJavac() {
        JClassReference list = TS.tryResolve(List.class);
        JExpressionInformation diamond = new JExpressionInformation.Instantiation.Anonymous(HOLDER, null, TS.tryResolve(ArrayList.class), List.of(), List.of(), List.of(), null);
        assertEquals(true, solves(diamond, list.parameterized(List.of((JArgumentType) TS.tryResolve(String.class)))), "List<String> x = new ArrayList<>() {}");
        assertEquals(true, solves(anonymous(Runnable.class, List.of()), TS.tryResolve(Runnable.class)), "Runnable x = new Runnable() { ... }");
        assertEquals(false, solves(anonymous(Holder.Closed.class, List.of()), TS.constants().object()), "Object x = new Closed() {}");
        assertEquals(false, solves(anonymous(Holder.Iface.class, List.of(c(5))), TS.constants().object()), "Object x = new Iface(5) {}");
        assertEquals(false, solves(anonymous(Holder.Sealed.class, List.of()), TS.constants().object()), "Object x = new Sealed() {}");
    }

    @Test
    public void sealedCastsFollowJavac() {
        JExpressionInformation open = new JExpressionInformation.Instantiation.Simple(HOLDER, null, TS.tryResolve(Holder.Open.class), List.of(), List.of());
        assertEquals(false, solves(cast(open, Holder.Sealed.class), TS.tryResolve(Holder.Sealed.class)), "Sealed x = (Sealed) new Open()");
        assertEquals(true, solves(cast(open, Holder.Iface.class), TS.tryResolve(Holder.Iface.class)), "Iface x = (Iface) new Open()");
        assertEquals(false, solves(cast(cast(c(null), Holder.Iface.class), Holder.Sealed.class), TS.tryResolve(Holder.Sealed.class)), "Sealed x = (Sealed) (Iface) null");
    }

    @Test
    public void unqualifiedInvocationFollowsJavac() throws NoSuchMethodException {
        JType object = TS.constants().object();
        assertEquals(false, solves(new JExpressionInformation.MethodInvocation.Simple<>(HOLDER, method("staticContext"), new JExpressionInformation.ImplicitReceiver(), "inst", List.of(), List.of()), TS.constants().voidType()), "static { inst(); }");
        assertEquals(true, solves(new JExpressionInformation.MethodInvocation.Simple<>(HOLDER, method("instanceContext"), new JExpressionInformation.ImplicitReceiver(), "inst", List.of(), List.of()), TS.constants().voidType()), "instance { inst(); }");
        assertEquals(false, solves(new JExpressionInformation.MethodInvocation.Simple<>(HOLDER, method("instanceContext"), new JExpressionInformation.ImplicitReceiver(), "inst", List.of(), List.of()), object), "Object x = inst()");
    }
}
