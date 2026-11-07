package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JWildType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class FindLeastUpperBoundTests {

    interface Foo<T extends Foo<T>> {}
    class Bar implements Foo<Bar> {}
    class Baz implements Foo<Baz> {}
    interface Box<T> {}
    static class RawBoxUser implements Box {}
    static class TypedBoxUser implements Box<String> {}

    @Test
    public void testInfiniteLub() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference foo = ts.tryResolve(Foo.class);

        JWildType.Upper inner = ts.typeFactory().newUpperWildType();
        inner.setUpperBounds(Set.of());
        JParameterizedClassType innerFoo = foo.parameterized(List.of((JArgumentType) inner));
        JWildType.Upper outer = ts.typeFactory().newUpperWildType();
        outer.setUpperBounds(Set.of(innerFoo));
        JParameterizedClassType expected = foo.parameterized(List.of((JArgumentType) outer));

        JType bar = ts.tryResolve(Bar.class);
        JType baz = ts.tryResolve(Baz.class);
        JType lub = ts.operations().findLeastUpperBound(Set.of(bar, baz));

        assertTrue(lub.typeEquals(expected), "javac: lub(Bar, Baz) = Foo<? extends Foo<?>>, got " + lub.simpleName());
    }

    @Test
    public void testLeastContainingTypeArgumentUpperWildcardWithPlainType() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference box = ts.tryResolve(Box.class);
        JClassReference numberType = ts.tryResolve(Number.class);
        JClassReference integerType = ts.tryResolve(Integer.class);

        JWildType.Upper upperBound = ts.typeFactory().newUpperWildType();
        upperBound.setUpperBounds(Set.of(numberType));

        JParameterizedClassType left = ts.typeFactory().newParameterizedClassType();
        left.setClassReference(box);
        left.setTypeArguments(List.of(upperBound));

        JParameterizedClassType right = ts.typeFactory().newParameterizedClassType();
        right.setClassReference(box);
        right.setTypeArguments(List.of(integerType));

        JType lub = ts.operations().findLeastUpperBound(Set.of(left, right));

        assertTrue(lub instanceof JParameterizedClassType);
        JArgumentType arg = ((JParameterizedClassType) lub).typeArguments().get(0);
        assertTrue(arg instanceof JWildType.Upper, arg + " should be an upper wildcard");
        assertTrue(((JWildType.Upper) arg).upperBound().typeEquals(numberType),
                "expected ? extends Number, got ? extends " + ((JWildType.Upper) arg).upperBound());
    }

    @Test
    public void testLeastContainingTypeArgumentLowerWildcardWithPlainType() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference box = ts.tryResolve(Box.class);
        JClassReference numberType = ts.tryResolve(Number.class);
        JClassReference integerType = ts.tryResolve(Integer.class);

        JWildType.Lower lowerBound = ts.typeFactory().newLowerWildType();
        lowerBound.setLowerBounds(Set.of(integerType));

        JParameterizedClassType left = ts.typeFactory().newParameterizedClassType();
        left.setClassReference(box);
        left.setTypeArguments(List.of(lowerBound));

        JParameterizedClassType right = ts.typeFactory().newParameterizedClassType();
        right.setClassReference(box);
        right.setTypeArguments(List.of(numberType));

        JType lub = ts.operations().findLeastUpperBound(Set.of(left, right));

        assertTrue(lub instanceof JParameterizedClassType);
        JArgumentType arg = ((JParameterizedClassType) lub).typeArguments().get(0);
        assertTrue(arg instanceof JWildType.Lower, arg + " should be a lower wildcard");
        Set<JType> bounds = ((JWildType.Lower) arg).lowerBounds();
        assertTrue(bounds.size() == 1 && bounds.iterator().next().typeEquals(integerType), "javac: lub(Box<? super Integer>, Box<Number>) = Box<? super Integer>, got " + bounds);
    }

    @Test
    public void testLubOfIdenticallyParameterizedOperandsPreservesTypeArgument() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference arrayListRef = ts.tryResolve(java.util.ArrayList.class);
        JClassReference linkedListRef = ts.tryResolve(java.util.LinkedList.class);
        JClassReference stringRef = ts.tryResolve(String.class);
        JClassReference abstractListRef = ts.tryResolve(java.util.AbstractList.class);

        JParameterizedClassType arrayListOfString = arrayListRef.parameterized(List.of((JArgumentType) stringRef));
        JParameterizedClassType linkedListOfString = linkedListRef.parameterized(List.of((JArgumentType) stringRef));

        JType lub = ts.operations().findLeastUpperBound(Set.of(arrayListOfString, linkedListOfString));

        JParameterizedClassType abstractListMember = findIntersectionMember(lub, abstractListRef);
        assertNotNull(abstractListMember, "expected " + lub.simpleName() + " to contain a java.util.AbstractList member");
        assertTrue(abstractListMember.typeArguments().get(0).typeEquals(stringRef),
                "expected AbstractList<String>, got AbstractList<" + abstractListMember.typeArguments().get(0).simpleName() + ">");
    }

    @Test
    public void testLubWithRawOccurrenceStillWidensToWildcard() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;
        JClassReference box = ts.tryResolve(Box.class);
        JType rawUser = ts.tryResolve(RawBoxUser.class);
        JType typedUser = ts.tryResolve(TypedBoxUser.class);

        JType lub = ts.operations().findLeastUpperBound(Set.of(rawUser, typedUser));

        assertTrue(lub.typeEquals(box), "javac: lub(RawBoxUser, TypedBoxUser) = raw Box, got " + lub.simpleName());
    }

    private static JParameterizedClassType findIntersectionMember(JType lub, JClassReference classRef) {
        List<JType> children = lub instanceof JIntersectionType it ? List.copyOf(it.children()) : List.of(lub);
        for (JType child : children) {
            if (child instanceof JParameterizedClassType pct && pct.classReference().typeEquals(classRef)) {
                return pct;
            }
        }
        return null;
    }

}
