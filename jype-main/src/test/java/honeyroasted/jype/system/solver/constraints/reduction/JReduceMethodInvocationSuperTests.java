package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.type.JClassReference;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers (possibly qualified) super method invocations (JLS §15.12.1): {@code super.method()}
 * begins method search at the direct superclass (skipping the current class's own override),
 * and {@code Outer.super.method()} does the same starting from a lexically enclosing class's
 * superclass instead.
 */
public class JReduceMethodInvocationSuperTests {

    public static class GrandParent {
        public String greet() {
            return "grandparent";
        }
    }

    public static class Parent extends GrandParent {
        @Override
        public String greet() {
            return "parent";
        }

        public class Inner {
        }
    }

    public static class Child extends Parent {
    }

    public static class Unrelated {
    }

    private static ConstraintTree solveInvocation(JTypeSystem ts, JClassReference declaring, JExpressionInformation.SuperReference source, JClassReference targetType) {
        JExpressionInformation.MethodInvocation<JExpressionInformation.SuperReference> invocation =
                new JExpressionInformation.MethodInvocation.Simple<>(declaring, null, source, "greet", List.of(), Collections.emptyList());
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                invocation, JTypeConstraints.Compatible.Context.ASSIGNMENT, targetType);
        return ts.operations().inferenceSolver().bind(constraint).solve();
    }

    @Test
    public void unqualifiedSuperInvokesDirectSuperclassOverride() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference child = ts.tryResolve(Child.class);
        JClassReference parent = ts.tryResolve(Parent.class);
        JClassReference string = ts.tryResolve(String.class);

        ConstraintTree tree = solveInvocation(ts, child, new JExpressionInformation.SuperReference(child), string);

        List<ConstraintBranch> survivors = tree.branches().stream().filter(b -> b.status().isTrue()).toList();
        assertEquals(1, survivors.size(), "expected exactly one surviving branch");

        JTypeContext.ChosenMethod chosen = survivors.get(0).metadata().first(JTypeContext.ChosenMethod.class).get();
        assertTrue(chosen.chosen().outerClass().typeEquals(parent), "expected super.greet() from Child to resolve to Parent.greet()");
    }

    @Test
    public void qualifiedSuperInvokesEnclosingClassesSuperclassOverride() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference inner = ts.tryResolve(Parent.Inner.class);
        JClassReference parent = ts.tryResolve(Parent.class);
        JClassReference grandParent = ts.tryResolve(GrandParent.class);
        JClassReference string = ts.tryResolve(String.class);

        ConstraintTree tree = solveInvocation(ts, inner, new JExpressionInformation.SuperReference(parent), string);

        List<ConstraintBranch> survivors = tree.branches().stream().filter(b -> b.status().isTrue()).toList();
        assertEquals(1, survivors.size(), "expected exactly one surviving branch");

        JTypeContext.ChosenMethod chosen = survivors.get(0).metadata().first(JTypeContext.ChosenMethod.class).get();
        assertTrue(chosen.chosen().outerClass().typeEquals(grandParent),
                "expected Parent.super.greet() from Inner to resolve to GrandParent.greet(), skipping Parent's own override");
    }

    @Test
    public void superQualifierMustBeALexicallyEnclosingClass() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference child = ts.tryResolve(Child.class);
        JClassReference unrelated = ts.tryResolve(Unrelated.class);
        JClassReference string = ts.tryResolve(String.class);

        ConstraintTree tree = solveInvocation(ts, child, new JExpressionInformation.SuperReference(unrelated), string);

        assertTrue(tree.branches().stream().noneMatch(b -> b.status().isTrue()),
                "Unrelated.super.greet() should not resolve since Unrelated does not lexically enclose Child");
    }
}
