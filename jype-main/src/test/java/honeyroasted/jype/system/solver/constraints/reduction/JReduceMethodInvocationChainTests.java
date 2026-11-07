package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers a chained method invocation where the receiver expression is itself a method invocation
 * (e.g. {@code foo().bar()}) - {@code foo()}'s own return type isn't known up front, so
 * {@code JReduceMethodInvocation} must solve it (via its own sub-solve against a fresh
 * metavariable) before {@code bar()}'s overload resolution can even begin. Addresses the
 * "//TODO test this rigorously" left on that branch.
 */
public class JReduceMethodInvocationChainTests {

    public interface Chain {
        Chain next();

        int value();

        String describe(int x);

        String describe(String x);
    }

    private static boolean solves(JTypeSystem ts, JExpressionInformation expr, JType target) {
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                expr, JTypeConstraints.Compatible.Context.ASSIGNMENT, target);
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();
        return tree.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    @Test
    public void chainedInvocationResolvesThroughNonSimplyTypedReceiver() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference chain = ts.tryResolve(Chain.class);
        JType intType = ts.constants().intType();

        JExpressionInformation.MethodInvocation<?> next = new JExpressionInformation.MethodInvocation.Simple<>(
                chain, null, JExpressionInformation.of(chain), "next", List.of(), Collections.emptyList());
        JExpressionInformation.MethodInvocation<?> value = new JExpressionInformation.MethodInvocation.Simple<>(
                chain, null, next, "value", List.of(), Collections.emptyList());

        assertTrue(solves(ts, value, intType), "expected foo().next().value() to type-check as int through the chained (non-simply-typed) receiver");
    }

    @Test
    public void chainedInvocationRejectsWrongReturnType() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference chain = ts.tryResolve(Chain.class);
        JClassReference string = ts.tryResolve(String.class);

        JExpressionInformation.MethodInvocation<?> next = new JExpressionInformation.MethodInvocation.Simple<>(
                chain, null, JExpressionInformation.of(chain), "next", List.of(), Collections.emptyList());
        JExpressionInformation.MethodInvocation<?> value = new JExpressionInformation.MethodInvocation.Simple<>(
                chain, null, next, "value", List.of(), Collections.emptyList());

        assertFalse(solves(ts, value, string), "expected foo().next().value() (an int) to NOT be assignable to String");
    }

    @Test
    public void chainedInvocationRejectsUnknownMethodOnResolvedReceiver() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference chain = ts.tryResolve(Chain.class);
        JType intType = ts.constants().intType();

        JExpressionInformation.MethodInvocation<?> next = new JExpressionInformation.MethodInvocation.Simple<>(
                chain, null, JExpressionInformation.of(chain), "next", List.of(), Collections.emptyList());
        JExpressionInformation.MethodInvocation<?> missing = new JExpressionInformation.MethodInvocation.Simple<>(
                chain, null, next, "doesNotExist", List.of(), Collections.emptyList());

        assertFalse(solves(ts, missing, intType), "expected foo().next().doesNotExist() to be rejected - no such method on Chain");
    }

    @Test
    public void chainedInvocationResolvesOverloadOnNonSimplyTypedReceiver() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference chain = ts.tryResolve(Chain.class);
        JClassReference string = ts.tryResolve(String.class);
        JType intType = ts.constants().intType();

        JExpressionInformation.MethodInvocation<?> next = new JExpressionInformation.MethodInvocation.Simple<>(
                chain, null, JExpressionInformation.of(chain), "next", List.of(), Collections.emptyList());
        JExpressionInformation.MethodInvocation<?> describe = new JExpressionInformation.MethodInvocation.Simple<>(
                chain, null, next, "describe", List.of(JExpressionInformation.of(intType)), Collections.emptyList());

        assertTrue(solves(ts, describe, string), "expected foo().next().describe(anInt) to resolve the int-overload of describe() and type-check as String");
    }
}
