package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JMostSpecificFunctionalTests {
    public interface F1<A, B> {
        B apply(A a);
    }

    public interface F2<A, B> {
        B apply(A a);
    }

    public interface S1<T> {
        T get();
    }

    public interface S2<T> {
        T get();
    }

    public static class Host {
        public static boolean flag;

        public static void a(F1<String, Integer> f) {
        }

        public static void a(F2<String, Number> f) {
        }

        public static void b(S1<Integer> f) {
        }

        public static void b(S2<Number> f) {
        }

        public static void e(S1<S1<Integer>> f) {
        }

        public static void e(S2<S2<Number>> f) {
        }

        public static Integer gi() {
            return 1;
        }
    }

    private static final JTypeSystem TS = JTypeSystem.RUNTIME_REFLECTION;
    private static final JClassReference HOST = TS.tryResolve(Host.class);

    private static boolean resolves(String name, JExpressionInformation arg) {
        JExpressionInformation call = new JExpressionInformation.MethodInvocation.Simple<>(HOST, null, HOST, name, List.of(arg), List.of());
        ConstraintTree tree = TS.operations().inferenceSolver().bind(new JTypeConstraints.ExpressionCompatible(call, JTypeConstraints.Compatible.Context.ASSIGNMENT, TS.constants().voidType())).solve();
        return tree.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    private static JExpressionInformation c(Object v) {
        return new JExpressionInformation.Constant.Simple(v);
    }

    private static JExpressionInformation supplier(JExpressionInformation body) {
        return new JExpressionInformation.Lambda.Simple(List.of(), null, new JExpressionInformation.LambdaBody.Expression(body));
    }

    private static JExpressionInformation block(List<JExpressionInformation> returns, boolean canCompleteNormally) {
        return new JExpressionInformation.Lambda.Simple(List.of(), null, new JExpressionInformation.LambdaBody.Block(List.of(), returns, false, canCompleteNormally));
    }

    @Test
    public void functionalInterfaceMostSpecificMatchesJavac() {
        JExpressionInformation flag = new JExpressionInformation.GetField.Simple<>(HOST, null, HOST, "flag");
        JExpressionInformation.LambdaParameter s = JExpressionInformation.LambdaParameter.fresh("s");
        JExpressionInformation implicit = new JExpressionInformation.Lambda.Simple(List.of(s), null, new JExpressionInformation.LambdaBody.Expression(c(1)));
        JExpressionInformation explicit = new JExpressionInformation.Lambda.Simple(List.of(s), List.of(TS.tryResolve(String.class)), new JExpressionInformation.LambdaBody.Expression(c(1)));

        assertEquals(false, resolves("a", implicit), "a(s -> 1) is ambiguous");
        assertEquals(true, resolves("a", explicit), "a((String s) -> 1)");
        assertEquals(true, resolves("b", supplier(c(1))), "b(() -> 1)");
        assertEquals(true, resolves("b", block(List.of(c(1), c(2)), false)), "b(() -> { if (flag) return 1; return 2; })");
        assertEquals(true, resolves("b", new JExpressionInformation.Conditional.Simple(flag, supplier(c(1)), supplier(c(2)))), "b(flag ? () -> 1 : () -> 2)");
        assertEquals(true, resolves("e", supplier(supplier(c(1)))), "e(() -> () -> 1)");
        assertEquals(false, resolves("b", new JExpressionInformation.Switch(c(1), List.of(supplier(c(1))), false)), "b(switch (1) { default -> () -> 1; }) is ambiguous in javac");
        assertEquals(true, resolves("b", block(List.of(), false)), "b(() -> { throw new RuntimeException(); })");
        assertEquals(true, resolves("b", new JExpressionInformation.InvocationReference.Simple<>(HOST, "gi", List.of())), "b(Host::gi)");
        assertEquals(true, resolves("b", supplier(new JExpressionInformation.MethodInvocation.Simple<>(HOST, null, HOST, "gi", List.of(), List.of()))), "b(() -> gi())");
    }
}
