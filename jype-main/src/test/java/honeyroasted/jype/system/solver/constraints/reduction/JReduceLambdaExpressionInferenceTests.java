package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassReference;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers JLS §18.5.2's "pertinent to applicability" exclusion: an implicitly typed lambda's
 * argument types can't be known until its target type's own parameter types are known, so when
 * that target type depends on another, not-yet-inferred type variable of the SAME invocation
 * (e.g. Consumer&lt;T&gt; where T is inferred from a sibling argument), the lambda must not be
 * rejected outright just because its target isn't a proper type yet.
 */
public class JReduceLambdaExpressionInferenceTests {

    public static class ConsumerHost {
        public static <T> void invoke(Consumer<T> c, T value) {
        }
    }

    public static class FunctionHost {
        public static <T> void invoke(Function<T, String> f, T value) {
        }
    }

    private static JExpressionInformation constant(Object value) {
        return new JExpressionInformation.Constant.Simple(value);
    }

    private static ConstraintTree solve(JTypeSystem ts, JClassReference host, JExpressionInformation.Lambda lambda, JExpressionInformation second) {
        JExpressionInformation.MethodInvocation<?> invocation = new JExpressionInformation.MethodInvocation.Simple<>(
                host, null, host, "invoke", List.of(lambda, second), Collections.emptyList());
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                invocation, JTypeConstraints.Compatible.Context.ASSIGNMENT, ts.constants().voidType());
        return ts.operations().inferenceSolver().bind(constraint).solve();
    }

    @Test
    public void implicitlyTypedLambdaWithGenericFunctionalParameterInfersFromSiblingArgument() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(ConsumerHost.class);

        JExpressionInformation.Lambda lambda = new JExpressionInformation.Lambda.Simple(constant(0), null, 1, false);
        ConstraintTree tree = solve(ts, host, lambda, constant("hello"));

        assertTrue(tree.branches().stream().anyMatch(b -> b.status().isTrue()),
                "expected invoke(x -> {}, \"hello\") to resolve, inferring T=String for the lambda's Consumer<T> parameter");
    }

    @Test
    public void implicitlyTypedLambdaBodyIsStillVerifiedOnceTheTargetTypeResolves() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference host = ts.tryResolve(FunctionHost.class);

        JExpressionInformation.Lambda matching = new JExpressionInformation.Lambda.Simple(constant("literal"), null, 1, true);
        ConstraintTree matchingTree = solve(ts, host, matching, constant("hello"));
        assertTrue(matchingTree.branches().stream().anyMatch(b -> b.status().isTrue()),
                "expected invoke(x -> \"literal\", \"hello\") to resolve since the body matches Function<T, String>'s String return type");

        JExpressionInformation.Lambda mismatched = new JExpressionInformation.Lambda.Simple(constant(42), null, 1, true);
        ConstraintTree mismatchedTree = solve(ts, host, mismatched, constant("hello"));
        assertTrue(mismatchedTree.branches().stream().noneMatch(b -> b.status().isTrue()),
                "expected invoke(x -> 42, \"hello\") to be rejected once T=String is inferred, since int is not compatible with the SAM's String return type");
    }
}
