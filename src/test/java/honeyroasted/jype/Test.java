package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver._old.solvers.inference.helper.TypeBoundResolver;
import honeyroasted.jype.system.solver._old.solvers.inference.helper.TypeConstraintReducer;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.impl.MetaVarTypeImpl;

import java.util.List;
import java.util.Set;

public class Test {

    public static <T> void main(String[] args) {
        TypeSystem system = TypeSystem.RUNTIME;
        TypeConstraintReducer reducer = new TypeConstraintReducer();
        TypeBoundResolver resolver = new TypeBoundResolver();

        ClassReference list = system.tryResolve(List.class);

        TypeBound.Result.Builder builder = TypeBound.Result.builder(new TypeBound.Compatible(
                list.parameterized(new MetaVarTypeImpl(system, "T")),
                list.parameterized(system.<ArgumentType>tryResolve(String.class))
        ));

        reducer.reduce(Set.of(builder));
        resolver.resolve(reducer.bounds());

        resolver.instantiations().forEach((mvt, t) -> System.out.println(mvt.toString() + " = " + t.toString()));

    }

}
