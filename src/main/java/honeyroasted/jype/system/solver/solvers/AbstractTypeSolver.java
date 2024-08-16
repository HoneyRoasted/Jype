package honeyroasted.jype.system.solver.solvers;

import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.bounds.TypeBound;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractTypeSolver implements TypeSolver {
    private Set<Class<? extends TypeBound>> supported;
    private String name;
    protected Set<TypeBound> constraints = new LinkedHashSet<>();

    public AbstractTypeSolver(String name, Set<Class<? extends TypeBound>> supported) {
        this.supported = supported;
        this.name = name;
    }

    public AbstractTypeSolver(Set<Class<? extends TypeBound>> supported) {
        this.supported = supported;
        this.name = getClass().getSimpleName();
    }

    @Override
    public boolean supports(TypeBound bound) {
        if (bound == null) return false;
        return this.supported.stream().anyMatch(c -> c.isInstance(bound));
    }

    @Override
    public TypeSolver bind(TypeBound bound) {
        Objects.requireNonNull(bound);
        if (!this.supports(bound)) {
            throw new IllegalArgumentException(this.name + " does not support TypeBound of type " +
                    bound.getClass().getCanonicalName() + ", supported bounds are: [" +
                    supported.stream().map(Class::getCanonicalName).collect(Collectors.joining(", ")) + "]");
        }
        this.constraints.add(bound);
        return this;
    }

    @Override
    public void reset() {
        this.constraints.clear();
    }

}
