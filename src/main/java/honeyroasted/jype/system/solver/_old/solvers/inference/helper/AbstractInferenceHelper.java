package honeyroasted.jype.system.solver._old.solvers.inference.helper;

import honeyroasted.jype.system.TypeOperations;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.solvers.NoOpTypeSolver;

public class AbstractInferenceHelper {
    protected TypeSolver solver;

    public AbstractInferenceHelper() {
        this(new NoOpTypeSolver());
    }

    public AbstractInferenceHelper(TypeSolver solver) {
        this.solver = solver;
    }

}
