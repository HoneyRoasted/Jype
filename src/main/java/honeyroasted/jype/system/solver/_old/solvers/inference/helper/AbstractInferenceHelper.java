package honeyroasted.jype.system.solver._old.solvers.inference.helper;

import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.solvers.TypeSolvers;

public class AbstractInferenceHelper {
    protected TypeSolver solver;

    public AbstractInferenceHelper() {
        this(TypeSolvers.NO_OP);
    }

    public AbstractInferenceHelper(TypeSolver solver) {
        this.solver = solver;
    }

}
