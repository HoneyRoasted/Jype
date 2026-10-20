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
 * Covers JLS §18.2.6/§8.4.6 for lambda bodies specifically: a lambda whose body is itself a
 * method/constructor invocation must not be assignable to a functional interface whose SAM
 * doesn't cover whatever checked exception that invocation might throw. Unlike a plain
 * method/constructor reference (where the referenced member is known up front), the lambda body's
 * invocation must first be resolved (choosing an overload) before its checked exceptions are even
 * known - see {@code JReduceLambdaExpression.resolveInvocationBodyWithThrowsCheck}.
 */
public class JReduceLambdaThrowsTests {

    public interface NoThrowsSupplier {
        String get();
    }

    public interface IOExceptionSupplier {
        String get() throws java.io.IOException;
    }

    public interface NoThrowsRunnable {
        void run();
    }

    public interface IOExceptionRunnable {
        void run() throws java.io.IOException;
    }

    public interface NoThrowsCtorSupplier {
        ThrowsIOException get();
    }

    public interface IOExceptionCtorSupplier {
        ThrowsIOException get() throws java.io.IOException;
    }

    public static class ThrowsIOException {
        public ThrowsIOException() throws java.io.IOException {
        }

        public static String checked() throws java.io.IOException {
            return null;
        }

        public static String uncheckedOnly() {
            return null;
        }
    }

    private static boolean solves(JTypeSystem ts, JExpressionInformation expr, JType target) {
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                expr, JTypeConstraints.Compatible.Context.ASSIGNMENT, target);
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();
        return tree.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    private static JExpressionInformation.Lambda lambdaCalling(JClassReference host, String method) {
        JExpressionInformation.MethodInvocation<?> body = new JExpressionInformation.MethodInvocation.Simple<>(
                host, null, host, method, List.of(), Collections.emptyList());
        return new JExpressionInformation.Lambda.Simple(body, null, 0, true);
    }

    @Test
    public void lambdaBodyThrowingCheckedExceptionRejectedByNoThrowsSam() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(ThrowsIOException.class);
        JClassReference target = ts.tryResolve(NoThrowsSupplier.class);

        JExpressionInformation.Lambda lambda = lambdaCalling(host, "checked");

        assertFalse(solves(ts, lambda, target),
                "expected () -> checked() to be rejected by a throws-nothing SAM - checked() throws IOException");
    }

    @Test
    public void lambdaBodyThrowingCheckedExceptionAcceptedByCoveringSam() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(ThrowsIOException.class);
        JClassReference target = ts.tryResolve(IOExceptionSupplier.class);

        JExpressionInformation.Lambda lambda = lambdaCalling(host, "checked");

        assertTrue(solves(ts, lambda, target),
                "expected () -> checked() to be accepted since the SAM's throws clause covers IOException");
    }

    @Test
    public void lambdaBodyWithNoCheckedExceptionsAcceptedByNoThrowsSam() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(ThrowsIOException.class);
        JClassReference target = ts.tryResolve(NoThrowsSupplier.class);

        JExpressionInformation.Lambda lambda = lambdaCalling(host, "uncheckedOnly");

        assertTrue(solves(ts, lambda, target),
                "expected () -> uncheckedOnly() to be accepted regardless of the SAM's throws clause - it throws nothing checked");
    }

    @Test
    public void voidLambdaBodyThrowingCheckedExceptionRejectedByNoThrowsSam() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(ThrowsIOException.class);
        JClassReference target = ts.tryResolve(NoThrowsRunnable.class);

        JExpressionInformation.Lambda lambda = lambdaCalling(host, "checked");

        assertFalse(solves(ts, lambda, target),
                "expected () -> checked() (its result discarded) to still be rejected by a throws-nothing void SAM - evaluating the body still throws IOException");
    }

    @Test
    public void voidLambdaBodyThrowingCheckedExceptionAcceptedByCoveringSam() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(ThrowsIOException.class);
        JClassReference target = ts.tryResolve(IOExceptionRunnable.class);

        JExpressionInformation.Lambda lambda = lambdaCalling(host, "checked");

        assertTrue(solves(ts, lambda, target),
                "expected () -> checked() to be accepted by a void SAM whose throws clause covers IOException");
    }

    @Test
    public void constructorInvocationLambdaBodyThrowingCheckedExceptionRejectedByNoThrowsSam() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference ctorClass = ts.tryResolve(ThrowsIOException.class);
        JClassReference target = ts.tryResolve(NoThrowsCtorSupplier.class);

        JExpressionInformation.Instantiation body = new JExpressionInformation.Instantiation.Simple(
                ctorClass, null, ctorClass, List.of(), Collections.emptyList());
        JExpressionInformation.Lambda lambda = new JExpressionInformation.Lambda.Simple(body, null, 0, true);

        assertFalse(solves(ts, lambda, target),
                "expected () -> new ThrowsIOException() to be rejected by a throws-nothing SAM - the constructor throws IOException");
    }

    @Test
    public void constructorInvocationLambdaBodyThrowingCheckedExceptionAcceptedByCoveringSam() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference ctorClass = ts.tryResolve(ThrowsIOException.class);
        JClassReference target = ts.tryResolve(IOExceptionCtorSupplier.class);

        JExpressionInformation.Instantiation body = new JExpressionInformation.Instantiation.Simple(
                ctorClass, null, ctorClass, List.of(), Collections.emptyList());
        JExpressionInformation.Lambda lambda = new JExpressionInformation.Lambda.Simple(body, null, 0, true);

        assertTrue(solves(ts, lambda, target),
                "expected () -> new ThrowsIOException() to be accepted since the SAM's throws clause covers IOException");
    }

    @Test
    public void nonInvocationLambdaBodyIsUnaffectedByThrowsChecking() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference target = ts.tryResolve(NoThrowsSupplier.class);

        JExpressionInformation.Lambda lambda = new JExpressionInformation.Lambda.Simple(
                new JExpressionInformation.Constant.Simple("hello"), null, 0, true);

        assertTrue(solves(ts, lambda, target),
                "expected () -> \"hello\" to type-check normally - a constant body can't throw anything, so throws-checking doesn't apply");
    }
}
