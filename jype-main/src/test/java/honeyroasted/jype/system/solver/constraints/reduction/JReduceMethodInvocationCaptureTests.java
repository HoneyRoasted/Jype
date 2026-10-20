package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JWildType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers JLS §5.1.10: accessing a member through a wildcard-parameterized receiver must apply
 * capture conversion, replacing the wildcard with a fresh type variable bounded per JLS §18.3.2
 * rather than exposing the raw wildcard - so the captured value is known to be *some* subtype of
 * the wildcard's upper bound, not the upper bound type itself.
 */
public class JReduceMethodInvocationCaptureTests {

    public interface Box<T> {
        T get();

        void set(T value);
    }

    private static JParameterizedClassType boxOfExtendsNumber(JTypeSystem ts) {
        JClassReference box = ts.tryResolve(Box.class);
        JParameterizedClassType boxType = ts.typeFactory().newParameterizedClassType();
        boxType.setClassReference(box);

        JWildType.Upper wtu = ts.typeFactory().newUpperWildType();
        wtu.setUpperBounds(Set.of(ts.tryResolve(Number.class)));
        boxType.setTypeArguments(List.of(wtu));

        return boxType;
    }

    private static boolean solvesGet(JTypeSystem ts, JParameterizedClassType boxType, JType target) {
        JClassReference declaring = ts.tryResolve(Box.class);
        JExpressionInformation.MethodInvocation<?> invocation = new JExpressionInformation.MethodInvocation.Simple<>(
                declaring, null, JExpressionInformation.of(boxType), "get", List.of(), Collections.emptyList());
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                invocation, JTypeConstraints.Compatible.Context.ASSIGNMENT, target);
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();
        return tree.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    @Test
    public void capturedWildcardResultIsCompatibleWithItsUpperBound() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JParameterizedClassType boxType = boxOfExtendsNumber(ts);
        JClassReference number = ts.tryResolve(Number.class);

        assertTrue(solvesGet(ts, boxType, number),
                "expected Box<? extends Number>.get() to be assignable to its captured upper bound Number");
    }

    @Test
    public void capturedWildcardResultIsNotCompatibleWithASpecificSubtypeOfItsUpperBound() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JParameterizedClassType boxType = boxOfExtendsNumber(ts);
        JClassReference integer = ts.tryResolve(Integer.class);

        assertFalse(solvesGet(ts, boxType, integer),
                "expected Box<? extends Number>.get() to NOT be assignable to Integer - the captured type could be any subtype of Number, not necessarily Integer");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    public void setWithResultOfGetOnExtendsWildcardTypeChecks() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JParameterizedClassType boxType = boxOfExtendsNumber(ts);
        JClassReference declaring = ts.tryResolve(Box.class);

        JExpressionInformation.MethodInvocation<?> getInvocation = new JExpressionInformation.MethodInvocation.Simple<>(
                declaring, null, JExpressionInformation.of(boxType), "get", List.of(), Collections.emptyList());
        JExpressionInformation.MethodInvocation<?> setInvocation = new JExpressionInformation.MethodInvocation.Simple<>(
                declaring, null, JExpressionInformation.of(boxType), "set", List.of(getInvocation), Collections.emptyList());
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                setInvocation, JTypeConstraints.Compatible.Context.ASSIGNMENT, ts.constants().voidType());
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();
        boolean solved = tree.branches().stream().anyMatch(b -> b.status().isTrue());

        assertTrue(solved, "expected box.set(box.get()) to type-check - passing a captured wildcard's own producer output back as the consumer argument is always sound. "
                + "(Formerly hung the solver indefinitely - see TYPE_SYSTEM_REVIEW_NOTES.md 'Session 3'/'Session 4' - hence the @Timeout as a tripwire, not because a timeout is an acceptable outcome.)");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    public void setOnExtendsWildcardWithUnrelatedValueIsRejected() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JParameterizedClassType boxType = boxOfExtendsNumber(ts);
        JClassReference declaring = ts.tryResolve(Box.class);
        JClassReference integer = ts.tryResolve(Integer.class);

        JExpressionInformation.MethodInvocation<?> setInvocation = new JExpressionInformation.MethodInvocation.Simple<>(
                declaring, null, JExpressionInformation.of(boxType), "set", List.of(JExpressionInformation.of(integer)), Collections.emptyList());
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                setInvocation, JTypeConstraints.Compatible.Context.ASSIGNMENT, ts.constants().voidType());
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();
        boolean solved = tree.branches().stream().anyMatch(b -> b.status().isTrue());

        assertFalse(solved, "expected box.set(anIntegerValue) to be rejected for box: Box<? extends Number> - the captured type could be any subtype of Number, not necessarily Integer (PECS)");
    }

    @Test
    public void wildcardMemberAccessRecordsACaptureConstraint() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JParameterizedClassType boxType = boxOfExtendsNumber(ts);
        JClassReference declaring = ts.tryResolve(Box.class);
        JClassReference number = ts.tryResolve(Number.class);

        JExpressionInformation.MethodInvocation<?> invocation = new JExpressionInformation.MethodInvocation.Simple<>(
                declaring, null, JExpressionInformation.of(boxType), "get", List.of(), Collections.emptyList());
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                invocation, JTypeConstraints.Compatible.Context.ASSIGNMENT, number);
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();

        boolean sawCapture = tree.branches().stream()
                .filter(b -> b.status().isTrue())
                .flatMap(b -> b.constraints().keySet().stream())
                .anyMatch(c -> c instanceof JTypeConstraints.Capture);

        assertTrue(sawCapture, "expected member access through a wildcard-parameterized receiver to trigger capture conversion (JLS §5.1.10)");
    }
}
