package honeyroasted.jype.system.solver.solvers;

import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractTypeSolver implements TypeSolver, TypeSolverListener {
    private Set<Class<? extends TypeBound>> supported;
    protected Set<TypeBound> initialBounds = new LinkedHashSet<>();

    protected List<TypeSolverListener> listeners = new ArrayList<>();

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
    public void boundCreated(TypeBound.ResultView bound) {
        this.listeners.forEach(l -> l.boundCreated(bound));
    }

    @Override
    public void boundSatisfied(TypeBound.ResultView bound) {
        this.listeners.forEach(l -> l.boundSatisfied(bound));
    }

    @Override
    public void boundUnsatisfied(TypeBound.ResultView bound) {
        this.listeners.forEach(l -> l.boundUnsatisfied(bound));
    }

    @Override
    public void solved(Result result) {
        this.listeners.forEach(l -> l.solved(result));
    }

    protected TypeBound.Result.Builder eventBoundCreated(TypeBound.Result.Builder bound) {
        this.boundCreated(bound);
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
        this.boundSatisfied(bound);
        return bound;
    }

    protected TypeBound.Result.Builder eventBoundUnsatisfied(TypeBound.Result.Builder bound) {
        this.boundUnsatisfied(bound);
        return bound;
    }

    protected TypeSolver.Result eventSolved(TypeSolver.Result result) {
        this.solved(result);
        return result;
    }

    @Override
    public TypeSolver addListener(TypeSolverListener listener) {
        this.listeners.add(listener);
        return this;
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
