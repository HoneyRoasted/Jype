package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.Constraint;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JWildType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit tests for {@link JResolveMostSpecificMethod#captureConvert}, the JLS §5.1.10
 * type-level capture conversion used by {@link JResolveMostSpecificMethod}'s §18.5.4
 * functional-interface comparison (see JResolveMostSpecificMethodGenericTests for the
 * higher-level "which overload wins" scenarios this feeds into).
 *
 * Tested directly against the bound math rather than only through full overload-resolution
 * scenarios: real javac (verified with JDK 24) treats *every* generic-vs-generic functional
 * interface comparison whose formal parameters mention the compared type parameter as ambiguous
 * regardless of whether capture is involved (the §18.5.4 formal-parameter bullet's generated
 * equality constraint is very easy to fail to satisfy), which makes it difficult to isolate
 * capture conversion's effect through a "which overload wins" style test alone - so the actual
 * bound computation is verified in isolation here instead.
 */
public class JResolveMostSpecificMethodCaptureTests {

    public interface Box<T> {
        T get();
    }

    private static JParameterizedClassType boxOf(JTypeSystem ts, honeyroasted.jype.type.JArgumentType typeArgument) {
        JClassReference box = ts.tryResolve(Box.class);
        JParameterizedClassType boxType = ts.typeFactory().newParameterizedClassType();
        boxType.setClassReference(box);
        boxType.setTypeArguments(List.of(typeArgument));
        return boxType;
    }

    @Test
    public void captureOfExtendsWildcardProducesFreshVarBoundedByTheWildcardsUpperBound() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference number = ts.tryResolve(Number.class);

        JWildType.Upper wtu = ts.typeFactory().newUpperWildType();
        wtu.setUpperBounds(Set.of(number));
        JParameterizedClassType boxType = boxOf(ts, wtu);

        List<Constraint> constraints = new ArrayList<>();
        JType captured = JResolveMostSpecificMethod.captureConvert(ts, boxType, constraints);

        assertInstanceOf(JParameterizedClassType.class, captured);
        JParameterizedClassType capturedBox = (JParameterizedClassType) captured;
        assertInstanceOf(JMetaVarType.class, capturedBox.typeArguments().get(0));

        JMetaVarType x = (JMetaVarType) capturedBox.typeArguments().get(0);
        assertTrue(x.upperBound().typeEquals(number), "expected the captured variable's upper bound to be Number");
        assertTrue(x.lowerBounds().isEmpty(), "expected the captured variable to have no lower bound (null type)");
        assertFalse(constraints.isEmpty(), "expected the captured bound to also be registered as a real Subtype constraint");
    }

    @Test
    public void captureOfSuperWildcardProducesFreshVarWithThatLowerBound() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference integer = ts.tryResolve(Integer.class);
        JClassReference object = ts.tryResolve(Object.class);

        JWildType.Lower wtl = ts.typeFactory().newLowerWildType();
        wtl.setLowerBounds(Set.of(integer));
        JParameterizedClassType boxType = boxOf(ts, wtl);

        List<Constraint> constraints = new ArrayList<>();
        JType captured = JResolveMostSpecificMethod.captureConvert(ts, boxType, constraints);

        JParameterizedClassType capturedBox = (JParameterizedClassType) captured;
        JMetaVarType x = (JMetaVarType) capturedBox.typeArguments().get(0);

        assertTrue(x.upperBound().typeEquals(object), "expected the captured variable's upper bound to fall back to Object (Box<T>'s own declared bound)");
        assertTrue(x.lowerBound().typeEquals(integer), "expected the captured variable's lower bound to be Integer");
    }

    @Test
    public void captureOfNonWildcardArgumentIsANoOp() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JParameterizedClassType boxType = boxOf(ts, ts.tryResolve(String.class));

        List<Constraint> constraints = new ArrayList<>();
        JType captured = JResolveMostSpecificMethod.captureConvert(ts, boxType, constraints);

        assertSame(boxType, captured, "expected capture conversion to be a no-op when there are no wildcard type arguments");
        assertTrue(constraints.isEmpty(), "expected no bounds to be generated when nothing was captured");
    }
}
