package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.solvers.TypeSolverListener;

import java.util.Collections;

public class NoOpTypeSolver implements TypeSolver {
    @Override
    public TypeSolver addListener(TypeSolverListener listener) {
        return this;
    }

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

    @Override
    public void boundCreated(TypeBound.ResultView bound) {

    }

    @Override
    public void boundSatisfied(TypeBound.ResultView bound) {

    }

    @Override
    public void boundUnsatisfied(TypeBound.ResultView bound) {

    }

    @Override
    public void solved(Result result) {

    }
}
