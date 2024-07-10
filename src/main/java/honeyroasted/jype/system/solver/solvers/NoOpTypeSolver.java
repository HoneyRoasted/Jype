package honeyroasted.jype.system.solver.solvers;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.bounds.TypeBound;

import java.util.Collections;

public class NoOpTypeSolver implements TypeSolver {
    @Override
    public boolean supports(TypeBound bound) {
        return true;
    }

    @Override
    public TypeSolver bind(TypeBound bound) {
        return this;
    }

    @Override
    public void reset() {

    }

    @Override
    public Result solve(TypeSystem system) {
        return new Result(true, Collections.emptySet());
    }

}
