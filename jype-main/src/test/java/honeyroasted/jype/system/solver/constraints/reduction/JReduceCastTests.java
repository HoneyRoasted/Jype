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
 * Covers explicit cast expressions {@code (T) operand} (JLS §5.5/§15.16): the cast itself must
 * be a legal conversion, and the cast's resulting type T must still satisfy whatever context the
 * cast expression is embedded in.
 */
public class JReduceCastTests {

    private static boolean solves(JTypeSystem ts, JExpressionInformation expr, JType target) {
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                expr, JTypeConstraints.Compatible.Context.ASSIGNMENT, target);
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();
        return tree.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    @Test
    public void validDowncastIsAccepted() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference object = ts.tryResolve(Object.class);
        JClassReference string = ts.tryResolve(String.class);

        JExpressionInformation.Cast cast = new JExpressionInformation.Cast.Simple(JExpressionInformation.of(object), string);

        assertTrue(solves(ts, cast, object), "expected (String) anObjectValue to be a legal cast");
    }

    @Test
    public void castBetweenUnrelatedFinalClassesIsRejected() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference string = ts.tryResolve(String.class);
        JClassReference integer = ts.tryResolve(Integer.class);
        JClassReference object = ts.tryResolve(Object.class);

        JExpressionInformation.Cast cast = new JExpressionInformation.Cast.Simple(JExpressionInformation.of(string), integer);

        assertFalse(solves(ts, cast, object), "expected (Integer) aStringValue to be an illegal cast between unrelated final classes");
    }

    @Test
    public void lambdaCastToFunctionalInterfaceIsAccepted() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference runnable = ts.tryResolve(Runnable.class);

        JExpressionInformation.Lambda lambda = new JExpressionInformation.Lambda.Simple(
                new JExpressionInformation.Constant.Simple(0), null, 0, false);
        JExpressionInformation.Cast cast = new JExpressionInformation.Cast.Simple(lambda, runnable);

        assertTrue(solves(ts, cast, runnable), "expected (Runnable) (() -> {}) to be a legal cast");
    }

    @Test
    public void lambdaCastToFunctionalInterfaceWithWrongParameterCountIsRejected() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference runnable = ts.tryResolve(Runnable.class);

        JExpressionInformation.Lambda lambda = new JExpressionInformation.Lambda.Simple(
                new JExpressionInformation.Constant.Simple(0), null, 1, false);
        JExpressionInformation.Cast cast = new JExpressionInformation.Cast.Simple(lambda, runnable);

        assertFalse(solves(ts, cast, runnable),
                "expected (Runnable) (x -> {}) to be rejected since Runnable.run() takes no parameters");
    }

    @Test
    public void validCastResultStillMustSatisfyTheOuterContext() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference object = ts.tryResolve(Object.class);
        JClassReference string = ts.tryResolve(String.class);
        JClassReference integer = ts.tryResolve(Integer.class);

        JExpressionInformation.Cast cast = new JExpressionInformation.Cast.Simple(JExpressionInformation.of(object), string);

        assertFalse(solves(ts, cast, integer),
                "expected (String) anObjectValue to still fail when the surrounding context requires an unrelated type (Integer), even though the cast itself is legal");
    }
}
