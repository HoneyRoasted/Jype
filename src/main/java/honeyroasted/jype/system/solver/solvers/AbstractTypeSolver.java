package honeyroasted.jype.system.solver.solvers;

import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractTypeSolver implements TypeSolver {
    private Set<Class<? extends TypeBound>> supported;
    private Set<Class<? extends TypeBound>> supportedAssumptions;
    protected Set<TypeBound> initialBounds = new LinkedHashSet<>();
    protected Set<TypeBound> assumedBounds = new LinkedHashSet<>();

    public AbstractTypeSolver(Set<Class<? extends TypeBound>> supported, Set<Class<? extends TypeBound>> supportedAssumptions) {
        this.supported = supported;
        this.supportedAssumptions = supportedAssumptions;
    }

    public AbstractTypeSolver(Class<? extends TypeBound>... supported) {
        this(Set.of(supported), Set.of(supported));
    }

    @Override
    public boolean supports(TypeBound bound) {
        return this.supported.stream().anyMatch(c -> c.isInstance(bound));
    }

    @Override
    public boolean supportsAssumption(TypeBound bound) {
        return this.supportedAssumptions.stream().anyMatch(c -> c.isInstance(bound));
    }

    @Override
    public TypeSolver bind(TypeBound bound) {
        if (!this.supports(bound))
            throw new IllegalArgumentException(getClass().getName() + " does not support TypeBound of type " +
                    (bound == null ? "null" : bound.getClass().getName()) + ", support bounds are: [" +
                    supported.stream().map(Class::getName).collect(Collectors.joining(", ")) + "]");
        this.initialBounds.add(bound);
        return this;
    }

    @Override
    public TypeSolver assume(TypeBound bound) {
        if (!this.supportsAssumption(bound))
            throw new IllegalArgumentException(getClass().getName() + " does not support assumption TypeBound of type " +
                    (bound == null ? "null" : bound.getClass().getName()) + ", support assumption bounds are: [" +
                    supported.stream().map(Class::getName).collect(Collectors.joining(", ")) + "]");
        this.assumedBounds.add(bound);
        return this;
    }
}
