package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers unary operator expressions (JLS §15.15): {@code +}/{@code -} (numeric, with unary
 * numeric promotion widening byte/short/char to int), {@code ~} (integral only), and {@code !}
 * (boolean only).
 */
public class JReduceUnaryOperatorTests {

    private static boolean solves(JTypeSystem ts, JExpressionInformation expr, JType target) {
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                expr, JTypeConstraints.Compatible.Context.ASSIGNMENT, target);
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();
        return tree.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    @Test
    public void unaryMinusOnIntIsInt() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();

        JExpressionInformation.UnaryOperator expr = new JExpressionInformation.UnaryOperator.Simple(
                JExpressionInformation.UnaryOperator.Kind.MINUS, JExpressionInformation.of(intType));

        assertTrue(solves(ts, expr, intType), "expected -anIntValue to type-check as int");
    }

    @Test
    public void unaryMinusOnBytePromotesToInt() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType byteType = ts.constants().byteType();
        JType intType = ts.constants().intType();

        JExpressionInformation.UnaryOperator expr = new JExpressionInformation.UnaryOperator.Simple(
                JExpressionInformation.UnaryOperator.Kind.MINUS, JExpressionInformation.of(byteType));

        assertTrue(solves(ts, expr, intType), "expected -aByteValue to type-check as int (unary numeric promotion)");
        assertFalse(solves(ts, expr, byteType), "expected -aByteValue to NOT still be a byte after promotion");
    }

    @Test
    public void bitwiseComplementRejectsBoolean() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();
        JType intType = ts.constants().intType();

        JExpressionInformation.UnaryOperator expr = new JExpressionInformation.UnaryOperator.Simple(
                JExpressionInformation.UnaryOperator.Kind.BITWISE_COMPLEMENT, JExpressionInformation.of(booleanType));

        assertFalse(solves(ts, expr, intType), "expected ~aBooleanValue to be rejected - ~ is only defined for integral types");
    }

    @Test
    public void bitwiseComplementOnIntIsInt() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();

        JExpressionInformation.UnaryOperator expr = new JExpressionInformation.UnaryOperator.Simple(
                JExpressionInformation.UnaryOperator.Kind.BITWISE_COMPLEMENT, JExpressionInformation.of(intType));

        assertTrue(solves(ts, expr, intType), "expected ~anIntValue to type-check as int");
    }

    @Test
    public void logicalComplementRequiresBoolean() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();
        JType intType = ts.constants().intType();

        JExpressionInformation.UnaryOperator legal = new JExpressionInformation.UnaryOperator.Simple(
                JExpressionInformation.UnaryOperator.Kind.LOGICAL_COMPLEMENT, JExpressionInformation.of(booleanType));
        JExpressionInformation.UnaryOperator illegal = new JExpressionInformation.UnaryOperator.Simple(
                JExpressionInformation.UnaryOperator.Kind.LOGICAL_COMPLEMENT, JExpressionInformation.of(intType));

        assertTrue(solves(ts, legal, booleanType), "expected !aBooleanValue to type-check as boolean");
        assertFalse(solves(ts, illegal, booleanType), "expected !anIntValue to be rejected - ! is only defined for boolean");
    }

    @Test
    public void nestedUnaryOperatorsCompose() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();

        JExpressionInformation.UnaryOperator inner = new JExpressionInformation.UnaryOperator.Simple(
                JExpressionInformation.UnaryOperator.Kind.MINUS, JExpressionInformation.of(intType));
        JExpressionInformation.UnaryOperator outer = new JExpressionInformation.UnaryOperator.Simple(
                JExpressionInformation.UnaryOperator.Kind.MINUS, inner);

        assertTrue(solves(ts, outer, intType), "expected -(-anIntValue) to type-check as int");
    }

    @Test
    public void illegalNestedCastPropagatesFailureThroughEnclosingOperator() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference string = ts.tryResolve(String.class);
        JClassReference integer = ts.tryResolve(Integer.class);
        JType intType = ts.constants().intType();

        JExpressionInformation.Cast illegalCast = new JExpressionInformation.Cast.Simple(JExpressionInformation.of(string), integer);
        JExpressionInformation.UnaryOperator expr = new JExpressionInformation.UnaryOperator.Simple(
                JExpressionInformation.UnaryOperator.Kind.MINUS, illegalCast);

        assertFalse(solves(ts, expr, intType),
                "expected -((Integer) aStringValue) to be rejected - the nested cast between unrelated final classes is illegal, and that illegality must propagate through the enclosing unary operator");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void bareLambdaOperandIsRejectedRatherThanHanging() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();

        JExpressionInformation.Lambda lambda = new JExpressionInformation.Lambda.Simple(
                new JExpressionInformation.Constant.Simple(0), null, 0, false);
        JExpressionInformation.UnaryOperator expr = new JExpressionInformation.UnaryOperator.Simple(
                JExpressionInformation.UnaryOperator.Kind.MINUS, lambda);

        assertFalse(solves(ts, expr, intType),
                "expected -(() -> 0) to be rejected - a lambda has no type without a target functional interface, so it can't be a unary operator operand; not valid Java either way");
    }
}
