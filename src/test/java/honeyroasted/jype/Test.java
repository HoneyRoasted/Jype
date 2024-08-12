package honeyroasted.jype;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;
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

    public static void main(String[] args) {
        TypeSystem system = TypeSystem.SIMPLE_RUNTIME;

        ClassReference list = (ClassReference) system.<ArgumentType>tryResolve(List.class);

        TypeBound.Result.Builder builder = TypeBound.Result.builder(new TypeBound.Compatible(
                list.parameterized(system.typeFactory().newMetaVarType("T")),
                list.parameterized(system.<ArgumentType>tryResolve(String.class))
        ));


        Pair<List<TypeBound.Result.Builder>, List<TypeBound.Result.Builder>> reduced = system.operations().reductionApplier().process(system, Collections.emptyList(), List.of(builder));
        Pair<Map<MetaVarType, Type>, Set<TypeBound.Result.Builder>> resolution = system.operations().resolveBounds(new LinkedHashSet<>(reduced.left()));

        resolution.left().forEach((mvt, t) -> System.out.println(mvt.toString() + " = " + t.toString()));
    }

}
