package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.fixtures.raw.RawGeneric;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JRawMemberErasureTests {
    @SuppressWarnings("rawtypes")
    public static RawGeneric raw;

    private static final JTypeSystem TS = JTypeSystem.RUNTIME_REFLECTION;
    private static final JClassReference HOST = TS.tryResolve(JRawMemberErasureTests.class);

    private static boolean solves(JExpressionInformation expr, JType target) {
        ConstraintTree tree = TS.operations().inferenceSolver().bind(new JTypeConstraints.ExpressionCompatible(expr, JTypeConstraints.Compatible.Context.ASSIGNMENT, target)).solve();
        return tree.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    private static JExpressionInformation call(String name, List<JArgumentType> typeArgs, Object... args) {
        JExpressionInformation receiver = new JExpressionInformation.GetField.Simple<>(HOST, null, HOST, "raw");
        List<JExpressionInformation> params = java.util.Arrays.stream(args).map(a -> (JExpressionInformation) new JExpressionInformation.Constant.Simple(a)).toList();
        return new JExpressionInformation.MethodInvocation.Simple<>(HOST, null, receiver, name, params, typeArgs);
    }

    @Test
    public void rawMembersAreErasedLikeJavac() {
        JType string = TS.tryResolve(String.class);
        JType object = TS.constants().object();
        assertEquals(true, solves(call("va", List.of(), 1, 2, 3), TS.constants().voidType()), "raw.va(1, 2, 3)");
        assertEquals(false, solves(call("gen", List.of(), "x"), string), "String s = raw.gen(\"x\")");
        assertEquals(true, solves(call("gen", List.of(), "x"), object), "Object s = raw.gen(\"x\")");
        assertEquals(true, solves(call("gen", List.of((JArgumentType) string), "x"), object), "Object s = raw.<String>gen(\"x\")");
        assertEquals(true, solves(call("bounded", List.of(), "x"), TS.tryResolve(Comparable.class)), "Comparable s = raw.bounded(\"x\")");
        assertEquals(false, solves(call("bounded", List.of(), "x"), string), "String s = raw.bounded(\"x\")");
        assertEquals(true, solves(call("get", List.of()), object), "Object o = raw.get()");
    }
}
