package honeyroasted.jype.system.solver.solvers;

import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractTypeSolver implements TypeSolver {
    private Set<Class<? extends TypeBound>> supported;
    protected Set<TypeBound> initialBounds = new LinkedHashSet<>();

    public AbstractTypeSolver(Set<Class<? extends TypeBound>> supported) {
        this.supported = supported;
    }

    public AbstractTypeSolver(Class<? extends TypeBound>... supported) {
        this(Set.of(supported));
    }

    @Override
    public boolean supports(TypeBound bound) {
        return this.supported.stream().anyMatch(c -> c.isInstance(bound));
    }

    @Override
    public TypeSolver bind(TypeBound bound) {
        if (!this.supports(bound)) throw new IllegalArgumentException(getClass().getName() + " does not support TypeBound of type " +
                (bound == null ? "null" : bound.getClass().getName()) + ", support bounds are: [" +
                supported.stream().map(Class::getName).collect(Collectors.joining(", ")) + "]");
        this.initialBounds.add(bound);
        return this;
    }

}
