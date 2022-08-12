package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.TypeSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a utility class that provides some common code for implementations of {@link TypeSolver}, such as
 * holding a reference to the relevant {@link TypeSystem}, and a list of accepted {@link TypeConstraint} types.
 */
public abstract class AbstractTypeSolver implements TypeSolver {
    protected TypeSystem system;
    protected List<TypeConstraint> constraints = Collections.synchronizedList(new ArrayList<>());

    protected Class<? extends TypeConstraint>[] acceptedConstraints;


    /**
     * A constructor to provide a reference to a {@link TypeSystem} and the list of acceptable {@link TypeConstraint}s
     * types.
     *
     * @param system              The {@link TypeSystem} associated with this {@link TypeSolver}
     * @param acceptedConstraints The types of {@link TypeConstraint}s this {@link TypeSolver} accepts
     */
    public AbstractTypeSolver(TypeSystem system, Class<? extends TypeConstraint>... acceptedConstraints) {
        this.system = system;
        this.acceptedConstraints = acceptedConstraints;
    }

    @Override
    public TypeSolver constrain(TypeConstraint constraint) {
        for (Class<? extends TypeConstraint> clazz : this.acceptedConstraints) {
            if (clazz.isInstance(constraint)) {
                this.constraints.add(constraint);
                return this;
            }
        }

        throw new IllegalArgumentException(getClass().getSimpleName() + " does not accept TypeConstraints of type " +
                (constraint == null ? "null" : constraint.getClass().getCanonicalName()) + ", accepted types are: [" +
                Arrays.stream(this.acceptedConstraints).map(Class::getCanonicalName).collect(Collectors.joining(", ")) + "]");
    }

    @Override
    public TypeSolver remove(TypeConstraint constraint) {
        this.constraints.remove(constraint);
        return this;
    }

    @Override
    public List<TypeConstraint> constraints() {
        return List.copyOf(this.constraints);
    }
}
