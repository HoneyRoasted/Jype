package honeyroasted.jype;

import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Test {

    interface Foo<T extends Foo<T>> {}

    class Bar implements Foo<Bar> {}
    class Baz implements Foo<Baz> {}

    public static void main(String[] args) {
        TypeSystem ts = TypeSystem.SIMPLE_RUNTIME;
        Type bar = ts.tryResolve(Bar.class);
        Type baz = ts.tryResolve(Baz.class);

        Type lub = ts.operations().findLeastUpperBound(Set.of(bar, baz));
        System.out.println(lub);
    }

}
