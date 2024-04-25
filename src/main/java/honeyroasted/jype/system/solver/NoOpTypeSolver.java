package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.TypeSystem;

import java.util.Collections;

public class NoOpTypeSolver implements TypeSolver {
    @Override
    public boolean supports(TypeBound bound) {
        return false;
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
        return new Result(Collections.emptySet());
    }

}
