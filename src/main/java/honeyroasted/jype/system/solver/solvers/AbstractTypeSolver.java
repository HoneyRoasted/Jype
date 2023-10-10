package honeyroasted.jype.system.solver.solvers;

import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;

import java.util.LinkedHashSet;
import java.util.Objects;
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

    private static TypeBound supportsImpl(TypeBound bound, Set<Class<? extends TypeBound>> supported) {
        if (supported.stream().noneMatch(c -> c.isInstance(bound))) {
            return bound;
        }

        if (bound instanceof TypeBound.Compound cmp) {
            for (TypeBound child : cmp.children()) {
                TypeBound subSupported = supportsImpl(child, supported);
                if (subSupported != null) {
                    return subSupported;
                }
            }
        }

        return null;
    }

    @Override
    public boolean supports(TypeBound bound) {
        if (bound == null) return false;
        return supportsImpl(bound, this.supported) == null;
    }

    @Override
    public boolean supportsAssumption(TypeBound bound) {
        if (bound == null) return false;
        return supportsImpl(bound, this.supportedAssumptions) == null;
    }

    @Override
    public TypeSolver bind(TypeBound bound) {
        Objects.requireNonNull(bound);
        TypeBound check = supportsImpl(bound, this.supported);
        if (check != null)
            throw new IllegalArgumentException(getClass().getName() + " does not support TypeBound of type " +
                    check.getClass().getName() + ", support bounds are: [" +
                    supported.stream().map(Class::getName).collect(Collectors.joining(", ")) + "]");
        this.initialBounds.add(bound);
        return this;
    }

    @Override
    public TypeSolver assume(TypeBound bound) {
        Objects.requireNonNull(bound);
        TypeBound check = supportsImpl(bound, this.supportedAssumptions);
        if (check != null)
            throw new IllegalArgumentException(getClass().getName() + " does not support assumption TypeBound of type " +
                    check + ", support assumption bounds are: [" +
                    supported.stream().map(Class::getName).collect(Collectors.joining(", ")) + "]");
        this.assumedBounds.add(bound);
        return this;
    }
}
