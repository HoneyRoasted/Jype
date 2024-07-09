package honeyroasted.jype.system.solver._old.solvers;

import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.bounds.TypeBound;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractTypeSolver implements TypeSolver {
    private Set<Class<? extends TypeBound>> supported;
    protected Set<TypeBound> initialBounds = new LinkedHashSet<>();

    public AbstractTypeSolver(Set<Class<? extends TypeBound>> supported) {
        this.supported = supported;
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

}
