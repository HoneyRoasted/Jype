package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.solvers.TypeInferenceSolver;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;

import java.util.List;

public class Test {

    public static void main(String[] args) {
        TypeSystem system = TypeSystem.SIMPLE_RUNTIME;

        ClassReference list = system.tryResolve(List.class);

        ParameterizedClassType param = list.parameterizedWithTypeVars();

        Type target = list.parameterized(system.<ArgumentType>tryResolve(String.class));

        System.out.println(new TypeInferenceSolver(true)
                        .bind(new TypeBound.Infer(param.typeArguments().get(0)))
                        .bind(new TypeBound.Compatible(param, target))
                .solve(system));
    }

}
