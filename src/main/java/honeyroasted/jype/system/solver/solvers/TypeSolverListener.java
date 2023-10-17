package honeyroasted.jype.system.solver.solvers;

import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;

public interface TypeSolverListener {

    void boundCreated(TypeBound.ResultView bound);

    void boundSatisfied(TypeBound.ResultView bound);

    void boundUnsatisfied(TypeBound.ResultView bound);

    void solved(TypeSolver.Result result);

}
