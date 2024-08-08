package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.type.Type;

import java.util.List;

public class Test {

    public static <T> void main(String[] args) {
        TypeSystem system = TypeSystem.SIMPLE_RUNTIME;

        Type subtype = new TypeToken<List<String>>(){}.resolve();
        Type supertype = new TypeToken<List<? extends Object>>(){}.resolve();

        TypeSolver.Result result = system.operations().compatibilitySolver()
                .bind(new TypeBound.Compatible(subtype, supertype, TypeBound.Compatible.Context.LOOSE_INVOCATION))
                .solve(system);

        System.out.println(result.toString(true));
    }

}
