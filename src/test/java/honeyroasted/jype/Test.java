package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.solvers.TypeSolvers;
import honeyroasted.jype.type.Type;

import java.util.List;

public class Test {

    public static void main(String[] args) {
        TypeSystem system = TypeSystem.RUNTIME;
        Type subtype = system.tryResolve(new TypeToken<List<String>>(){});
        Type supertype = system.tryResolve(new TypeToken<List<? extends CharSequence>>(){});

        TypeSolver typeSolver = TypeSolvers.COMPATIBILITY;
        System.out.println(typeSolver.bind(new TypeBound.Compatible(subtype, supertype))
                .solve(system).toString(true));
    }

}
