package honeyroasted.jype.system.solver.solvers.inference.helper;

import honeyroasted.jype.system.solver.TypeSolver;

public class AbstractInferenceHelper {
    protected TypeSolver solver;

    public AbstractInferenceHelper() {
        this(TypeSolver.NO_OP);
    }

    public AbstractInferenceHelper(TypeSolver solver) {
        this.solver = solver;
    }

}
