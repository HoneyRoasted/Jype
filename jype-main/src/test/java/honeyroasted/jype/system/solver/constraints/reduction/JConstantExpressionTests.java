package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JConstantExpressionTests {
    public static class Holder {
        public static final int CONST = 10;
        public static int nonConst = 10;
        public static boolean flag;
    }

    private static final JTypeSystem TS = JTypeSystem.RUNTIME_REFLECTION;

    private static boolean solves(JExpressionInformation expr, JType target) {
        ConstraintTree tree = TS.operations().inferenceSolver().bind(new JTypeConstraints.ExpressionCompatible(expr, JTypeConstraints.Compatible.Context.ASSIGNMENT, target)).solve();
        return tree.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    private static JExpressionInformation c(Object v) {
        return new JExpressionInformation.Constant.Simple(v);
    }

    private static JExpressionInformation bin(JExpressionInformation.BinaryOperator.Kind kind, JExpressionInformation l, JExpressionInformation r) {
        return new JExpressionInformation.BinaryOperator.Simple(kind, l, r);
    }

    private static JExpressionInformation field(String name) {
        JClassReference holder = TS.tryResolve(Holder.class);
        return new JExpressionInformation.GetField.Simple<>(holder, null, holder, name);
    }

    @Test
    public void constantExpressionsNarrowLikeJavac() {
        var k = TS.constants();
        var add = JExpressionInformation.BinaryOperator.Kind.ADD;
        assertEquals(true, solves(bin(add, c(1), c(2)), k.byteType()), "byte b = 1 + 2");
        assertEquals(false, solves(bin(add, c(127), c(1)), k.byteType()), "byte b = 127 + 1");
        assertEquals(true, solves(bin(add, c('a'), c(1)), k.charType()), "char c = 'a' + 1");
        assertEquals(true, solves(new JExpressionInformation.Cast.Simple(c(70000), k.shortType()), k.shortType()), "short s = (short) 70000");
        assertEquals(true, solves(field("CONST"), k.byteType()), "byte b = CONST");
        assertEquals(false, solves(field("nonConst"), k.byteType()), "byte b = nonConst");
        assertEquals(true, solves(bin(add, c(1), c(1)), TS.tryResolve(Byte.class)), "Byte b = 1 + 1");
        assertEquals(true, solves(new JExpressionInformation.Cast.Simple(c(2.5), k.intType()), k.byteType()), "byte b = (int) 2.5");
        assertEquals(false, solves(new JExpressionInformation.Conditional.Simple(field("flag"), c(1), c(2)), k.byteType()), "byte b = flag ? 1 : 2");
        assertEquals(true, solves(new JExpressionInformation.Conditional.Simple(c(true), c(1), c(2)), k.byteType()), "byte b = true ? 1 : 2");
        assertEquals(false, solves(c(-1), k.charType()), "char c = -1");
        assertEquals(false, solves(c(1L), k.byteType()), "byte b = 1L");
        assertEquals(true, solves(c(65), TS.tryResolve(Character.class)), "Character c = 65");
        assertEquals(true, solves(c('a'), k.shortType()), "short s = 'a'");
        assertEquals(true, solves(new JExpressionInformation.UnaryOperator.Simple(JExpressionInformation.UnaryOperator.Kind.BITWISE_COMPLEMENT, c(0)), k.byteType()), "byte b = ~0");
        assertEquals(false, solves(bin(JExpressionInformation.BinaryOperator.Kind.SHL, c(1), c(7)), k.byteType()), "byte b = 1 << 7");
    }
}
