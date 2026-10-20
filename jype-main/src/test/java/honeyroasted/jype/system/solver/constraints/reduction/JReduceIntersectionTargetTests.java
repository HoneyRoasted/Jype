package honeyroasted.jype.system.solver.constraints.reduction;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers lambda/method-reference/constructor-reference targets that are an intersection of
 * interfaces (JLS §9.8), e.g. a cast target {@code (Task & Marker)}.
 */
public class JReduceIntersectionTargetTests {

    public interface Marker {
    }

    public interface Task {
        void run();
    }

    public interface Producer<T> {
        T get();
    }

    public interface StrLen {
        int len(String s);
    }

    public static class Job {
        public Job() {
        }
    }

    private static JExpressionInformation constant(Object value) {
        return new JExpressionInformation.Constant.Simple(value);
    }

    private static boolean solves(JTypeSystem ts, JExpressionInformation expr, JType target) {
        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(
                expr, JTypeConstraints.Compatible.Context.ASSIGNMENT, target);
        ConstraintTree tree = ts.operations().inferenceSolver().bind(constraint).solve();
        return tree.branches().stream().anyMatch(b -> b.status().isTrue());
    }

    @Test
    public void lambdaSatisfiesIntersectionOfSamInterfaceAndMarker() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference task = ts.tryResolve(Task.class);
        JClassReference marker = ts.tryResolve(Marker.class);
        JType target = JIntersectionType.of(Set.of(task, marker), ts);

        JExpressionInformation.Lambda lambda = new JExpressionInformation.Lambda.Simple(constant(0), null, 0, false);

        assertTrue(solves(ts, lambda, target), "expected a 0-param lambda to satisfy (Task & Marker)");
    }

    @Test
    public void lambdaRejectsIntersectionWithTwoDistinctAbstractMethods() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference task = ts.tryResolve(Task.class);
        JClassReference producer = ts.tryResolve(Producer.class);
        JType target = JIntersectionType.of(Set.of(task, producer.parameterizedWithMetaVars()), ts);

        JExpressionInformation.Lambda lambda = new JExpressionInformation.Lambda.Simple(constant(0), null, 0, false);

        assertFalse(solves(ts, lambda, target), "an intersection with two unrelated abstract methods has no SAM");
    }

    @Test
    public void lambdaResolvesGenericSamThroughIntersectionMember() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference producerRef = ts.tryResolve(Producer.class);
        JClassReference marker = ts.tryResolve(Marker.class);
        JParameterizedClassType producerOfString = producerRef.parameterized(List.of(ts.tryResolve(String.class)));
        JType target = JIntersectionType.of(Set.of(producerOfString, marker), ts);

        JExpressionInformation.Lambda lambda = new JExpressionInformation.Lambda.Simple(constant("hello"), null, 0, true);

        assertTrue(solves(ts, lambda, target), "expected () -> \"hello\" to satisfy (Producer<String> & Marker)");
    }

    @Test
    public void lambdaRejectsGenericSamMismatchThroughIntersectionMember() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference producerRef = ts.tryResolve(Producer.class);
        JClassReference marker = ts.tryResolve(Marker.class);
        JParameterizedClassType producerOfString = producerRef.parameterized(List.of(ts.tryResolve(String.class)));
        JType target = JIntersectionType.of(Set.of(producerOfString, marker), ts);

        JExpressionInformation.Lambda lambda = new JExpressionInformation.Lambda.Simple(constant(42), null, 0, true);

        assertFalse(solves(ts, lambda, target), "int body is not compatible with the resolved return type String");
    }

    @Test
    public void constructorRefSatisfiesIntersectionOfSamInterfaceAndMarker() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference task = ts.tryResolve(Task.class);
        JClassReference marker = ts.tryResolve(Marker.class);
        JType target = JIntersectionType.of(Set.of(task, marker), ts);

        JExpressionInformation.InstantiationReference ctorRef =
                new JExpressionInformation.InstantiationReference.Simple(ts.tryResolve(Job.class), List.of());

        assertTrue(solves(ts, ctorRef, target), "expected Job::new to satisfy (Task & Marker)");
    }

    @Test
    public void methodRefSatisfiesIntersectionOfSamInterfaceAndMarker() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference marker = ts.tryResolve(Marker.class);
        JClassReference strLen = ts.tryResolve(StrLen.class);
        JType target = JIntersectionType.of(Set.of(strLen, marker), ts);

        // String::length as an unbound instance method reference (JLS §15.13 n=k+1 case):
        // the receiver becomes the SAM's first parameter.
        JExpressionInformation.InvocationReference<JClassReference> methodRef =
                new JExpressionInformation.InvocationReference.Simple<>(ts.tryResolve(String.class), "length", List.of());

        assertTrue(solves(ts, methodRef, target), "expected String::length to satisfy (StrLen & Marker)");
    }
}
