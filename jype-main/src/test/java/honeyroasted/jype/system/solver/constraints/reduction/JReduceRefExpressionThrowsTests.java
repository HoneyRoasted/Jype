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
 * Covers JLS §18.2.6/§8.4.6: a method/constructor reference must not be assignable to a
 * functional interface whose SAM declares no throws clause (or a narrower one) if the
 * referenced member might throw a checked exception the SAM doesn't permit.
 */
public class JReduceRefExpressionThrowsTests {

    public interface NoThrowsSupplier {
        String get();
    }

    public interface IOExceptionSupplier {
        String get() throws java.io.IOException;
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

    @Test
    public void methodRefThrowingCheckedExceptionRejectedByNoThrowsSam() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(ThrowsIOException.class);
        JClassReference target = ts.tryResolve(NoThrowsSupplier.class);

        JExpressionInformation.InvocationReference<JClassReference> ref =
                new JExpressionInformation.InvocationReference.Simple<>(host, "checked", Collections.emptyList());

        assertFalse(solves(ts, ref, target),
                "expected a checked-exception-throwing method reference to be rejected by a throws-nothing SAM");
    }

    @Test
    public void methodRefThrowingCheckedExceptionAcceptedByCoveringSam() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(ThrowsIOException.class);
        JClassReference target = ts.tryResolve(IOExceptionSupplier.class);

        JExpressionInformation.InvocationReference<JClassReference> ref =
                new JExpressionInformation.InvocationReference.Simple<>(host, "checked", Collections.emptyList());

        assertTrue(solves(ts, ref, target),
                "expected the method reference to be accepted since the SAM's throws clause covers IOException");
    }

    @Test
    public void methodRefWithNoCheckedExceptionsAcceptedByNoThrowsSam() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(ThrowsIOException.class);
        JClassReference target = ts.tryResolve(NoThrowsSupplier.class);

        JExpressionInformation.InvocationReference<JClassReference> ref =
                new JExpressionInformation.InvocationReference.Simple<>(host, "uncheckedOnly", Collections.emptyList());

        assertTrue(solves(ts, ref, target),
                "expected a method reference with no checked exceptions to be accepted regardless of the SAM's throws clause");
    }

    @Test
    public void constructorRefThrowingCheckedExceptionRejectedByNoThrowsSam() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference ctorClass = ts.tryResolve(ThrowsIOException.class);
        JClassReference target = ts.tryResolve(NoThrowsCtorSupplier.class);

        JExpressionInformation.InstantiationReference ref =
                new JExpressionInformation.InstantiationReference.Simple(ctorClass, List.of());

        assertFalse(solves(ts, ref, target),
                "expected a checked-exception-throwing constructor reference to be rejected by a throws-nothing SAM");
    }

    @Test
    public void constructorRefThrowingCheckedExceptionAcceptedByCoveringSam() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference ctorClass = ts.tryResolve(ThrowsIOException.class);
        JClassReference target = ts.tryResolve(IOExceptionCtorSupplier.class);

        JExpressionInformation.InstantiationReference ref =
                new JExpressionInformation.InstantiationReference.Simple(ctorClass, List.of());

        assertTrue(solves(ts, ref, target),
                "expected the constructor reference to be accepted since the SAM's throws clause covers IOException");
    }
}
