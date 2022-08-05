package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.TypeSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractTypeSolver implements TypeSolver {
    protected TypeSystem system;
    protected List<TypeConstraint> constraints = Collections.synchronizedList(new ArrayList<>());

    protected Class<? extends TypeConstraint>[] acceptedConstraints;


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
    public List<TypeConstraint> constraints() {
        return List.copyOf(this.constraints);
    }
}
