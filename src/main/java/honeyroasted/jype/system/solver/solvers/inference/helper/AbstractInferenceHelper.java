package honeyroasted.jype.system.solver.solvers.inference.helper;

import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;

public class AbstractInferenceHelper {
    protected TypeSolver solver;

    public AbstractInferenceHelper(TypeSolver solver) {
        this.solver = solver;
    }

    protected TypeBound.Result.Builder eventBoundCreated(TypeBound.Result.Builder bound) {
        this.solver.boundCreated(bound);
        return bound;
    }

    protected TypeBound.Result.Builder eventBoundSatisfiedOrUnsatisfied(TypeBound.Result.Builder bound) {
        if (bound.satisfied()) {
            return this.eventBoundSatisfied(bound);
        } else {
            return this.eventBoundUnsatisfied(bound);
        }
    }

    protected TypeBound.Result.Builder eventBoundSatisfied(TypeBound.Result.Builder bound) {
        this.solver.boundSatisfied(bound);
        return bound;
    }

    protected TypeBound.Result.Builder eventBoundUnsatisfied(TypeBound.Result.Builder bound) {
        this.solver.boundUnsatisfied(bound);
        return bound;
    }

    protected TypeBound.Result.Builder eventInsightDiscovered(TypeBound.Result.Builder bound) {
        this.solver.insightDiscovered(bound);
        return bound;
    }

    protected TypeSolver.Result eventSolved(TypeSolver.Result result) {
        this.solver.solved(result);
        return result;
    }
}
