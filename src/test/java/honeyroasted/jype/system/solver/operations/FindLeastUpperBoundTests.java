package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;
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
        TypeSystem ts = TypeSystem.SIMPLE_RUNTIME;

        //Create test type to compare too
        ClassReference foo = ts.tryResolve(Foo.class);
        ParameterizedClassType pct = ts.newParameterizedClassType();
        pct.setClassReference(foo);
        WildType.Upper wtu = ts.newUpperWildType();
        wtu.setUpperBounds(Set.of(pct));
        pct.setTypeArguments(List.of(wtu));

        //Create infinite lub
        Type bar = ts.tryResolve(Bar.class);
        Type baz = ts.tryResolve(Baz.class);
        Type lub = ts.operations().findLeastUpperBound(Set.of(bar, baz));

        //Set wild type identity to be the same, otherwise they will not be considered equal
        if (lub instanceof ParameterizedClassType lubPct && !lubPct.typeArguments().isEmpty() &&
                lubPct.typeArguments().get(0) instanceof WildType.Upper lubWtu) {
            wtu.setIdentity(lubWtu.identity());
        }

        assertTrue(lub.typeEquals(pct), lub.simpleName() + " = " + pct.simpleName());
    }

}
