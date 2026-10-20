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
 * Covers the {@code instanceof} type comparison operator (JLS §15.20.2): the comparison is only
 * legal between types that could possibly be related (the same castability rule as an explicit
 * cast, per JLS §15.20.2's explicit deference to §5.5), and the expression's own type is always
 * {@code boolean}.
 */
public class JReduceInstanceOfTests {

    private static boolean solves(JTypeSystem ts, JExpressionInformation expr, JType target) {
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                expr, JTypeConstraints.Compatible.Context.ASSIGNMENT, target);
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();
        return tree.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    @Test
    public void instanceOfBetweenRelatedTypesIsLegalAndBoolean() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference object = ts.tryResolve(Object.class);
        JClassReference string = ts.tryResolve(String.class);
        JType booleanType = ts.constants().booleanType();

        JExpressionInformation.InstanceOf instOf = new JExpressionInformation.InstanceOf.Simple(JExpressionInformation.of(object), string);

        assertTrue(solves(ts, instOf, booleanType), "expected (anObjectValue instanceof String) to type-check as boolean");
    }

    @Test
    public void instanceOfBetweenUnrelatedFinalClassesIsRejected() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference string = ts.tryResolve(String.class);
        JClassReference integer = ts.tryResolve(Integer.class);
        JType booleanType = ts.constants().booleanType();

        JExpressionInformation.InstanceOf instOf = new JExpressionInformation.InstanceOf.Simple(JExpressionInformation.of(string), integer);

        assertFalse(solves(ts, instOf, booleanType),
                "expected (aStringValue instanceof Integer) to be rejected - String and Integer are unrelated final classes, so this can never be true");
    }

    @Test
    public void instanceOfWithInterfaceOperandTypeIsAlwaysLegal() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference comparable = ts.tryResolve(Comparable.class);
        JClassReference string = ts.tryResolve(String.class);
        JType booleanType = ts.constants().booleanType();

        JExpressionInformation.InstanceOf instOf = new JExpressionInformation.InstanceOf.Simple(JExpressionInformation.of(comparable), string);

        assertTrue(solves(ts, instOf, booleanType),
                "expected (aComparableValue instanceof String) to be legal - an interface-typed operand could always be some implementing class");
    }

    @Test
    public void instanceOfResultCannotBeAssignedToANonBooleanType() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference object = ts.tryResolve(Object.class);
        JClassReference string = ts.tryResolve(String.class);

        JExpressionInformation.InstanceOf instOf = new JExpressionInformation.InstanceOf.Simple(JExpressionInformation.of(object), string);

        assertFalse(solves(ts, instOf, string), "expected the instanceof expression's boolean result to not be assignable to String");
    }
}
