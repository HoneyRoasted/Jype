package honeyroasted.jype.system.solver;

import honeyroasted.jype.TypeConcrete;

import java.util.List;

/**
 * This interface represents a class which can perform some logical process on a set of {@link TypeConstraint}s and
 * produce a solution. The simplest use would be to determine assignability between two {@link TypeConcrete}s,
 * but that is not required behavior. It just represents any sufficiently complex task to perform on some types.
 * <p>
 * Note that not every TypeSolver supports every {@link TypeConstraint}, see individual documentation to determine
 * what constraints are supported. Also, each instance of a TypeSolver should remember all the {@link TypeConstraint}s
 * it has been provided. If a new set of {@link TypeConstraint}s needs to be processed, a new instance of that
 * TypeSolver should be created.
 * <p>
 * Implementations of TypeSolver should be thread-safe.
 */
public interface TypeSolver {

    /**
     * Adds a {@link TypeConstraint} to this {@link TypeSolver}. Note that not all {@link TypeSolver}s support all
     * types of {@link TypeConstraint}s.
     *
     * @param constraint The {@link TypeConstraint} to add
     * @return This, for method chaining
     */
    TypeSolver constrain(TypeConstraint constraint);

    /**
     * Removes a {@link TypeConstraint} from this {@link TypeSolver}.
     *
     * @param constraint The {@link TypeConstraint} to remove
     * @return This, for method chaining
     */
    TypeSolver remove(TypeConstraint constraint);

    /**
     * Attempts to produce a valid {@link TypeSolution} from the {@link TypeConstraint}s currently held in this
     * {@link TypeSolver}. In general, implementations of this method should not throw exceptions, but rather
     * report errors through a failed {@link TypeSolution}.
     *
     * @return The {@link TypeSolution} generated from this {@link TypeSolver}
     */
    TypeSolution solve();

    /**
     * @return The {@link List} of {@link TypeConstraint}s this {@link TypeSolver} is currently considering. The returned
     * list may be a copy and/or immutable
     */
    List<TypeConstraint> constraints();

}
