package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JClassReference;
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

    @Test
    public void testInfiniteLub() {
        JTypeSystem ts = JTypeSystem.RUNTIME_REFLECTION;

        //Create test type to compare too
        JClassReference foo = ts.tryResolve(Foo.class);
        JParameterizedClassType pct = ts.typeFactory().newParameterizedClassType();
        pct.setClassReference(foo);
        JWildType.Upper wtu = ts.typeFactory().newUpperWildType();
        wtu.setUpperBounds(Set.of(pct));
        pct.setTypeArguments(List.of(wtu));

        //Create infinite lub
        JType bar = ts.tryResolve(Bar.class);
        JType baz = ts.tryResolve(Baz.class);
        JType lub = ts.operations().findLeastUpperBound(Set.of(bar, baz));

        //Set wild type identity to be the same, otherwise they will not be considered equal
        if (lub instanceof JParameterizedClassType lubPct && !lubPct.typeArguments().isEmpty() &&
                lubPct.typeArguments().get(0) instanceof JWildType.Upper lubWtu) {
            wtu.setIdentity(lubWtu.identity());
        }

        assertTrue(lub.typeEquals(pct), lub.simpleName() + " = " + pct.simpleName());
    }

}
