package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers binary operator expressions (JLS §15.17-§15.24): arithmetic with binary numeric
 * promotion and String concatenation, shift, relational, equality (numeric/boolean/reference),
 * and bitwise/logical/conditional operators.
 */
public class JReduceBinaryOperatorTests {

    private static boolean solves(JTypeSystem ts, JExpressionInformation expr, JType target) {
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                expr, JTypeConstraints.Compatible.Context.ASSIGNMENT, target);
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();
        return tree.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    private static JExpressionInformation.BinaryOperator binOp(JExpressionInformation.BinaryOperator.Kind kind, JType left, JType right) {
        return new JExpressionInformation.BinaryOperator.Simple(kind, JExpressionInformation.of(left), JExpressionInformation.of(right));
    }

    @Test
    public void additionOfIntAndLongPromotesToLong() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();
        JType longType = ts.constants().longType();

        JExpressionInformation.BinaryOperator expr = binOp(JExpressionInformation.BinaryOperator.Kind.ADD, intType, longType);

        assertTrue(solves(ts, expr, longType), "expected anIntValue + aLongValue to type-check as long");
        assertFalse(solves(ts, expr, intType), "expected anIntValue + aLongValue to NOT be an int (widened to long)");
    }

    @Test
    public void additionWithStringOperandConcatenatesToString() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference string = ts.tryResolve(String.class);
        JType intType = ts.constants().intType();

        JExpressionInformation.BinaryOperator expr = binOp(JExpressionInformation.BinaryOperator.Kind.ADD, string, intType);

        assertTrue(solves(ts, expr, string), "expected aStringValue + anIntValue to type-check as String (concatenation)");
    }

    @Test
    public void subtractionRejectsBoolean() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();
        JType intType = ts.constants().intType();

        JExpressionInformation.BinaryOperator expr = binOp(JExpressionInformation.BinaryOperator.Kind.SUB, booleanType, intType);

        assertFalse(solves(ts, expr, intType), "expected aBooleanValue - anIntValue to be rejected - boolean isn't numeric");
    }

    @Test
    public void shiftResultTypeIsThePromotedLeftOperandType() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();
        JType longType = ts.constants().longType();
        JType byteType = ts.constants().byteType();
        JType booleanType = ts.constants().booleanType();

        JExpressionInformation.BinaryOperator intShiftedByLong = binOp(JExpressionInformation.BinaryOperator.Kind.SHL, intType, longType);
        assertTrue(solves(ts, intShiftedByLong, intType), "expected anIntValue << aLongValue to type-check as int (shift distance doesn't affect result type)");
        assertFalse(solves(ts, intShiftedByLong, booleanType), "expected anIntValue << aLongValue to NOT be boolean");

        JExpressionInformation.BinaryOperator byteShifted = binOp(JExpressionInformation.BinaryOperator.Kind.SHL, byteType, intType);
        assertTrue(solves(ts, byteShifted, intType), "expected aByteValue << anIntValue to promote to int (unary numeric promotion of the left operand)");
    }

    @Test
    public void shiftRejectsFloatingPointOperand() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();
        JType doubleType = ts.constants().doubleType();

        JExpressionInformation.BinaryOperator expr = binOp(JExpressionInformation.BinaryOperator.Kind.SHL, doubleType, intType);

        assertFalse(solves(ts, expr, doubleType), "expected aDoubleValue << anIntValue to be rejected - shift operators require integral operands");
    }

    @Test
    public void relationalOperatorsYieldBoolean() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();
        JType doubleType = ts.constants().doubleType();
        JType booleanType = ts.constants().booleanType();

        JExpressionInformation.BinaryOperator expr = binOp(JExpressionInformation.BinaryOperator.Kind.LT, intType, doubleType);

        assertTrue(solves(ts, expr, booleanType), "expected anIntValue < aDoubleValue to type-check as boolean");
    }

    @Test
    public void relationalOperatorsRejectNonNumericOperands() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference string = ts.tryResolve(String.class);
        JType intType = ts.constants().intType();
        JType booleanType = ts.constants().booleanType();

        JExpressionInformation.BinaryOperator expr = binOp(JExpressionInformation.BinaryOperator.Kind.LT, string, intType);

        assertFalse(solves(ts, expr, booleanType), "expected aStringValue < anIntValue to be rejected - < requires numeric operands");
    }

    @Test
    public void equalityBetweenBooleansIsLegal() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();

        JExpressionInformation.BinaryOperator expr = binOp(JExpressionInformation.BinaryOperator.Kind.EQ, booleanType, booleanType);

        assertTrue(solves(ts, expr, booleanType), "expected aBooleanValue == anotherBooleanValue to type-check as boolean");
    }

    @Test
    public void equalityBetweenRelatedReferenceTypesIsLegal() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference object = ts.tryResolve(Object.class);
        JClassReference string = ts.tryResolve(String.class);
        JType booleanType = ts.constants().booleanType();

        JExpressionInformation.BinaryOperator expr = binOp(JExpressionInformation.BinaryOperator.Kind.EQ, object, string);

        assertTrue(solves(ts, expr, booleanType), "expected anObjectValue == aStringValue to type-check as boolean - Object could hold a String");
    }

    @Test
    public void equalityBetweenUnrelatedFinalClassesIsRejected() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference string = ts.tryResolve(String.class);
        JClassReference integer = ts.tryResolve(Integer.class);
        JType booleanType = ts.constants().booleanType();

        JExpressionInformation.BinaryOperator expr = binOp(JExpressionInformation.BinaryOperator.Kind.EQ, string, integer);

        assertFalse(solves(ts, expr, booleanType), "expected aStringValue == anIntegerValue to be rejected - unrelated final classes can never be equal");
    }

    @Test
    public void bitwiseOperatorsAcceptBothBooleanOrBothIntegral() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();
        JType intType = ts.constants().intType();
        JType longType = ts.constants().longType();

        JExpressionInformation.BinaryOperator booleanAnd = binOp(JExpressionInformation.BinaryOperator.Kind.AND, booleanType, booleanType);
        assertTrue(solves(ts, booleanAnd, booleanType), "expected aBooleanValue & anotherBooleanValue to type-check as boolean");

        JExpressionInformation.BinaryOperator integralOr = binOp(JExpressionInformation.BinaryOperator.Kind.OR, intType, longType);
        assertTrue(solves(ts, integralOr, longType), "expected anIntValue | aLongValue to type-check as long");
    }

    @Test
    public void bitwiseOperatorsRejectMixedBooleanAndNumeric() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();
        JType intType = ts.constants().intType();

        JExpressionInformation.BinaryOperator expr = binOp(JExpressionInformation.BinaryOperator.Kind.XOR, booleanType, intType);

        assertFalse(solves(ts, expr, intType), "expected aBooleanValue ^ anIntValue to be rejected - mixed boolean/numeric operands");
    }

    @Test
    public void bitwiseOperatorsRejectFloatingPoint() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();
        JType doubleType = ts.constants().doubleType();

        JExpressionInformation.BinaryOperator expr = binOp(JExpressionInformation.BinaryOperator.Kind.AND, intType, doubleType);

        assertFalse(solves(ts, expr, doubleType), "expected anIntValue & aDoubleValue to be rejected - & is not defined for floating point types");
    }

    @Test
    public void conditionalAndRequiresBothBoolean() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();
        JType intType = ts.constants().intType();

        JExpressionInformation.BinaryOperator legal = binOp(JExpressionInformation.BinaryOperator.Kind.COND_AND, booleanType, booleanType);
        JExpressionInformation.BinaryOperator illegal = binOp(JExpressionInformation.BinaryOperator.Kind.COND_AND, booleanType, intType);

        assertTrue(solves(ts, legal, booleanType), "expected aBooleanValue && anotherBooleanValue to type-check as boolean");
        assertFalse(solves(ts, illegal, booleanType), "expected aBooleanValue && anIntValue to be rejected - && requires both operands boolean");
    }

    @Test
    public void nestedBinaryOperatorsCompose() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();
        JType longType = ts.constants().longType();

        JExpressionInformation.BinaryOperator inner = binOp(JExpressionInformation.BinaryOperator.Kind.ADD, intType, intType);
        JExpressionInformation.BinaryOperator outer = new JExpressionInformation.BinaryOperator.Simple(
                JExpressionInformation.BinaryOperator.Kind.ADD, inner, JExpressionInformation.of(longType));

        assertTrue(solves(ts, outer, longType), "expected (anIntValue + anotherIntValue) + aLongValue to type-check as long");
    }

    @Test
    public void illegalNestedCastPropagatesFailureThroughEnclosingOperator() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference string = ts.tryResolve(String.class);
        JClassReference integer = ts.tryResolve(Integer.class);
        JType intType = ts.constants().intType();

        JExpressionInformation.Cast illegalCast = new JExpressionInformation.Cast.Simple(JExpressionInformation.of(string), integer);
        JExpressionInformation.BinaryOperator expr = new JExpressionInformation.BinaryOperator.Simple(
                JExpressionInformation.BinaryOperator.Kind.ADD, illegalCast, JExpressionInformation.of(intType));

        assertFalse(solves(ts, expr, intType),
                "expected ((Integer) aStringValue) + anIntValue to be rejected - the nested cast is illegal, and that must propagate through the enclosing binary operator");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void bareMethodReferenceOperandIsRejectedRatherThanHanging() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference string = ts.tryResolve(String.class);
        JType intType = ts.constants().intType();

        JExpressionInformation.InvocationReference<?> methodRef = new JExpressionInformation.InvocationReference.Simple<>(
                string, "valueOf", Collections.emptyList());
        JExpressionInformation.BinaryOperator expr = new JExpressionInformation.BinaryOperator.Simple(
                JExpressionInformation.BinaryOperator.Kind.ADD, methodRef, JExpressionInformation.of(intType));

        assertFalse(solves(ts, expr, intType),
                "expected String::valueOf + anIntValue to be rejected - a method reference has no type without a target functional interface, so it can't be a binary operator operand; not valid Java either way");
    }
}
