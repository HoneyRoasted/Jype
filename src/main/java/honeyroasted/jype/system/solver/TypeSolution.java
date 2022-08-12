package honeyroasted.jype.system.solver;

import java.util.List;

/**
 * This class represents a solution produced by a {@link TypeSolver}. It may or may not be successful, and contains a
 * {@link TypeVerification} to indicate the success value, and, if it was unsuccessful, what {@link TypeConstraint}s
 * caused the failure.
 */
public class TypeSolution {
    private TypeContext context;
    private List<TypeConstraint> constraints;
    private TypeVerification verification;

    /**
     * Creates a new {@link TypeSolution}
     *
     * @param context      The {@link TypeContext} making up this {@link TypeSolution}
     * @param constraints  The {@link TypeConstraint}s that gave rise to this {@link TypeSolution}
     * @param verification The {@link TypeVerification} that contains the success value and information for
     *                     what constraints led to success or failure
     */
    public TypeSolution(TypeContext context, List<TypeConstraint> constraints, TypeVerification verification) {
        this.context = context;
        this.constraints = constraints;
        this.verification = verification;
    }

    /**
     * @return The {@link TypeContext} providing information about the solution reached by the relevant {@link TypeSolver}.
     * It may be empty if this {@link TypeSolution} was a failure, or there is no relevant information to provide
     */
    public TypeContext context() {
        return this.context;
    }

    /**
     * @return The {@link TypeConstraint}s that gave rise to this {@link TypeSolution}
     */
    public List<TypeConstraint> constraints() {
        return this.constraints;
    }

    /**
     * @return The {@link TypeVerification} associated with this {@link TypeSolution}. It contains the success value and
     * information for what constraints led to success or failure
     */
    public TypeVerification verification() {
        return this.verification;
    }

    /**
     * @return Whether this {@link TypeSolution} was successful. Equivalent to {@code verification().success()}
     */
    public boolean successful() {
        return this.verification.success();
    }
}
