package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers array-creation expressions (JLS §15.10): both the dimension-expression form
 * ({@code new T[e1][e2]...}, where each {@code eN} must be int-compatible per §15.10.1) and the
 * initializer form ({@code new T[]{...}}, where each element must be assignment-compatible with
 * the component type, recursively for nested initializers).
 */
public class JReduceArrayCreationTests {

    private static boolean solves(JTypeSystem ts, JExpressionInformation expr, JType target) {
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                expr, JTypeConstraints.Compatible.Context.ASSIGNMENT, target);
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();
        return tree.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    private static JType arrayOf(JType component, int dims, JTypeSystem ts) {
        JExpressionInformation.ArrayCreation ac = new JExpressionInformation.ArrayCreation.Simple(component, List.of(), dims, null);
        return ac.arrayType(ts);
    }

    @Test
    public void simpleDimensionFormTypeChecks() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();

        JExpressionInformation.ArrayCreation expr = new JExpressionInformation.ArrayCreation.Simple(
                intType, List.of(new JExpressionInformation.Constant.Simple(5)), 0, null);

        assertTrue(solves(ts, expr, arrayOf(intType, 1, ts)), "expected new int[5] to type-check as int[]");
    }

    @Test
    public void trailingDimensionlessBracketsAddDepth() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();

        JExpressionInformation.ArrayCreation expr = new JExpressionInformation.ArrayCreation.Simple(
                intType, List.of(new JExpressionInformation.Constant.Simple(5)), 1, null);

        assertTrue(solves(ts, expr, arrayOf(intType, 2, ts)), "expected new int[5][] to type-check as int[][]");
        assertFalse(solves(ts, expr, arrayOf(intType, 1, ts)), "expected new int[5][] to NOT be int[]");
    }

    @Test
    public void nonIntCompatibleDimensionExpressionIsRejected() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();
        JClassReference string = ts.tryResolve(String.class);

        JExpressionInformation.ArrayCreation expr = new JExpressionInformation.ArrayCreation.Simple(
                intType, List.of(JExpressionInformation.of(string)), 0, null);

        assertFalse(solves(ts, expr, arrayOf(intType, 1, ts)), "expected new int[aStringValue] to be rejected - a String isn't a valid array dimension");
    }

    @Test
    public void longDimensionExpressionIsRejected() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();
        JType longType = ts.constants().longType();

        JExpressionInformation.ArrayCreation expr = new JExpressionInformation.ArrayCreation.Simple(
                intType, List.of(JExpressionInformation.of(longType)), 0, null);

        assertFalse(solves(ts, expr, arrayOf(intType, 1, ts)), "expected new int[aLongValue] to be rejected - long is not a valid array dimension type per JLS 15.10.1");
    }

    @Test
    public void byteDimensionExpressionWidensToInt() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();
        JType byteType = ts.constants().byteType();

        JExpressionInformation.ArrayCreation expr = new JExpressionInformation.ArrayCreation.Simple(
                intType, List.of(JExpressionInformation.of(byteType)), 0, null);

        assertTrue(solves(ts, expr, arrayOf(intType, 1, ts)), "expected new int[aByteValue] to be legal - byte widens to int");
    }

    @Test
    public void initializerFormChecksElementsAgainstComponentType() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();

        JExpressionInformation.ArrayInitializer init = new JExpressionInformation.ArrayInitializer.Simple(
                List.of(new JExpressionInformation.Constant.Simple(1), new JExpressionInformation.Constant.Simple(2)));
        JExpressionInformation.ArrayCreation expr = new JExpressionInformation.ArrayCreation.Simple(
                intType, List.of(), 1, init);

        assertTrue(solves(ts, expr, arrayOf(intType, 1, ts)), "expected new int[]{1, 2} to type-check as int[]");
    }

    @Test
    public void initializerFormRejectsWrongElementType() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();
        JClassReference string = ts.tryResolve(String.class);

        JExpressionInformation.ArrayInitializer init = new JExpressionInformation.ArrayInitializer.Simple(
                List.of(new JExpressionInformation.Constant.Simple(1), JExpressionInformation.of(string)));
        JExpressionInformation.ArrayCreation expr = new JExpressionInformation.ArrayCreation.Simple(
                intType, List.of(), 1, init);

        assertFalse(solves(ts, expr, arrayOf(intType, 1, ts)), "expected new int[]{1, aStringValue} to be rejected - a String isn't assignable to int");
    }

    @Test
    public void nestedInitializersRecurseForMultiDimensionalArrays() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();

        JExpressionInformation.ArrayInitializer innerA = new JExpressionInformation.ArrayInitializer.Simple(
                List.of(new JExpressionInformation.Constant.Simple(1), new JExpressionInformation.Constant.Simple(2)));
        JExpressionInformation.ArrayInitializer innerB = new JExpressionInformation.ArrayInitializer.Simple(
                List.of(new JExpressionInformation.Constant.Simple(3)));
        JExpressionInformation.ArrayInitializer outer = new JExpressionInformation.ArrayInitializer.Simple(List.of(innerA, innerB));

        JExpressionInformation.ArrayCreation expr = new JExpressionInformation.ArrayCreation.Simple(
                intType, List.of(), 2, outer);

        assertTrue(solves(ts, expr, arrayOf(intType, 2, ts)), "expected new int[][]{{1, 2}, {3}} to type-check as int[][]");
    }

    @Test
    public void nestedInitializerWithWrongElementTypeIsRejected() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JType intType = ts.constants().intType();
        JClassReference string = ts.tryResolve(String.class);

        JExpressionInformation.ArrayInitializer inner = new JExpressionInformation.ArrayInitializer.Simple(
                List.of(JExpressionInformation.of(string)));
        JExpressionInformation.ArrayInitializer outer = new JExpressionInformation.ArrayInitializer.Simple(List.of(inner));

        JExpressionInformation.ArrayCreation expr = new JExpressionInformation.ArrayCreation.Simple(
                intType, List.of(), 2, outer);

        assertFalse(solves(ts, expr, arrayOf(intType, 2, ts)), "expected new int[][]{{aStringValue}} to be rejected - a String isn't assignable to int");
    }
}
