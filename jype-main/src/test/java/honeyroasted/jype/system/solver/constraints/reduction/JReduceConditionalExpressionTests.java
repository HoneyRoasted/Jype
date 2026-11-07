package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers conditional expressions {@code condition ? whenTrue : whenFalse} (JLS §15.25): the
 * condition must be boolean, and the branches merge via boolean/numeric-promotion (including the
 * byte&amp;short-&gt;short special case and constant-narrowing) or - for reference types -
 * least-upper-bound after boxing. Poly branches (lambdas/method references) instead propagate the
 * outer context straight through to both sides.
 */
public class JReduceConditionalExpressionTests {

    private static boolean solves(JTypeSystem ts, JExpressionInformation expr, JType target) {
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                expr, JTypeConstraints.Compatible.Context.ASSIGNMENT, target);
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();
        return tree.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    private static JExpressionInformation.Conditional cond(JExpressionInformation condition, JType t, JType f) {
        return new JExpressionInformation.Conditional.Simple(condition, JExpressionInformation.of(t), JExpressionInformation.of(f));
    }

    @Test
    public void bothBooleanBranchesYieldBoolean() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();

        JExpressionInformation.Conditional expr = cond(JExpressionInformation.of(booleanType), booleanType, booleanType);

        assertTrue(solves(ts, expr, booleanType), "expected (aBooleanValue ? true : false) to type-check as boolean");
    }

    @Test
    public void conditionMustBeBoolean() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();
        JType intType = ts.constants().intType();

        JExpressionInformation.Conditional expr = cond(JExpressionInformation.of(intType), booleanType, booleanType);

        assertFalse(solves(ts, expr, booleanType), "expected an int-typed condition to be rejected - the condition must be boolean");
    }

    @Test
    public void byteAndShortBranchesMergeToShort() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();
        JType byteType = ts.constants().byteType();
        JType shortType = ts.constants().shortType();
        JType intType = ts.constants().intType();

        JExpressionInformation.Conditional expr = cond(JExpressionInformation.of(booleanType), byteType, shortType);

        assertTrue(solves(ts, expr, shortType), "expected (cond ? aByteValue : aShortValue) to type-check as short");
        assertFalse(solves(ts, expr, byteType), "expected (cond ? aByteValue : aShortValue) to NOT be byte");
    }

    @Test
    public void intAndLongBranchesPromoteToLong() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();
        JType intType = ts.constants().intType();
        JType longType = ts.constants().longType();

        JExpressionInformation.Conditional expr = cond(JExpressionInformation.of(booleanType), intType, longType);

        assertTrue(solves(ts, expr, longType), "expected (cond ? anIntValue : aLongValue) to type-check as long");
    }

    @Test
    public void constantIntRepresentableInByteNarrowsToByte() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();
        JType byteType = ts.constants().byteType();

        JExpressionInformation.Conditional expr = new JExpressionInformation.Conditional.Simple(
                JExpressionInformation.of(booleanType), JExpressionInformation.of(byteType), new JExpressionInformation.Constant.Simple(10));

        assertTrue(solves(ts, expr, byteType), "expected (cond ? aByteValue : 10) to narrow to byte since 10 fits in a byte");
    }

    @Test
    public void constantIntNotRepresentableInByteFallsBackToInt() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();
        JType byteType = ts.constants().byteType();
        JType intType = ts.constants().intType();

        JExpressionInformation.Conditional expr = new JExpressionInformation.Conditional.Simple(
                JExpressionInformation.of(booleanType), JExpressionInformation.of(byteType), new JExpressionInformation.Constant.Simple(1000));

        assertTrue(solves(ts, expr, intType), "expected (cond ? aByteValue : 1000) to be int since 1000 doesn't fit in a byte");
        assertFalse(solves(ts, expr, byteType), "expected (cond ? aByteValue : 1000) to NOT be byte");
    }

    @Test
    public void referenceBranchesMergeToLeastUpperBound() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();
        JClassReference string = ts.tryResolve(String.class);
        JClassReference integer = ts.tryResolve(Integer.class);
        JClassReference object = ts.tryResolve(Object.class);
        JClassReference serializable = ts.tryResolve(java.io.Serializable.class);

        JExpressionInformation.Conditional expr = cond(JExpressionInformation.of(booleanType), string, integer);

        assertTrue(solves(ts, expr, object), "expected (cond ? aStringValue : anIntegerValue) to type-check as Object");
        assertTrue(solves(ts, expr, serializable), "expected (cond ? aStringValue : anIntegerValue) to type-check as Serializable (a shared interface)");
    }

    @Test
    public void mixingBooleanAndNumericBranchesIsAReferenceConditional() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();
        JType intType = ts.constants().intType();
        JClassReference object = ts.tryResolve(Object.class);

        JExpressionInformation.Conditional expr = cond(JExpressionInformation.of(booleanType), booleanType, intType);

        assertTrue(solves(ts, expr, object), "javac: Object o = c ? aBoolean : anInt compiles (reference conditional, JLS 15.25.3)");
        assertFalse(solves(ts, expr, intType), "javac: int x = c ? aBoolean : anInt is rejected");
    }

    @Test
    public void polyLambdaBranchesPropagateTargetFunctionalInterface() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();
        JClassReference runnable = ts.tryResolve(Runnable.class);

        JExpressionInformation.Lambda trueLambda = new JExpressionInformation.Lambda.Simple(
                new JExpressionInformation.Constant.Simple(0), null, 0, false);
        JExpressionInformation.Lambda falseLambda = new JExpressionInformation.Lambda.Simple(
                new JExpressionInformation.Constant.Simple(0), null, 0, false);

        JExpressionInformation.Conditional expr = new JExpressionInformation.Conditional.Simple(
                JExpressionInformation.of(booleanType), trueLambda, falseLambda);

        assertTrue(solves(ts, expr, runnable), "expected (cond ? (() -> {}) : (() -> {})) to type-check as Runnable via poly propagation");
    }

    @Test
    public void nestedConditionalsCompose() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType booleanType = ts.constants().booleanType();
        JType intType = ts.constants().intType();
        JType longType = ts.constants().longType();

        JExpressionInformation.Conditional inner = cond(JExpressionInformation.of(booleanType), intType, intType);
        JExpressionInformation.Conditional outer = new JExpressionInformation.Conditional.Simple(
                JExpressionInformation.of(booleanType), inner, JExpressionInformation.of(longType));

        assertTrue(solves(ts, outer, longType), "expected (cond ? (cond2 ? anInt : anInt) : aLongValue) to type-check as long");
    }
}
