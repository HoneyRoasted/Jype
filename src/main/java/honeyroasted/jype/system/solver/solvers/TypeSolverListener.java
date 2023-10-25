package honeyroasted.jype.system.solver.solvers;

import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;

public interface TypeSolverListener {

    interface Default extends TypeSolverListener {

        @Override
        default void boundCreated(TypeBound.ResultView bound) {}

        @Override
        default void boundSatisfied(TypeBound.ResultView bound) {}

        @Override
        default void boundUnsatisfied(TypeBound.ResultView bound) {}

        @Override
        default void solved(TypeSolver.Result result) {}
    }

    void boundCreated(TypeBound.ResultView bound);

    void boundSatisfied(TypeBound.ResultView bound);

    void boundUnsatisfied(TypeBound.ResultView bound);

    void solved(TypeSolver.Result result);

}
