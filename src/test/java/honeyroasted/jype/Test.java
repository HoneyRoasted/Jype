package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.solvers.inference.helper.TypeBoundResolver;
import honeyroasted.jype.system.solver.solvers.inference.helper.TypeConstraintReducer;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.impl.MetaVarTypeImpl;

import java.util.List;
import java.util.Set;

public class Test {

    public static void main(String[] args) {
        TypeSystem system = TypeSystem.RUNTIME;
        TypeConstraintReducer reducer = new TypeConstraintReducer();
        TypeBoundResolver resolver = new TypeBoundResolver();

        ClassReference list = (ClassReference) system.resolve(List.class).get();

        TypeBound.Result.Builder builder = TypeBound.Result.builder(new TypeBound.Compatible(
                list.parameterized(new MetaVarTypeImpl(system, "T")),
                list.parameterized((ArgumentType) system.resolve(String.class).get())
        ));

        reducer.reduce(Set.of(builder));
        resolver.resolve(reducer.bounds());

        resolver.instantiations().forEach((mvt, t) -> System.out.println(mvt.toString() + " = " + t.toString()));
    }

}
