package honeyroasted.jype.system.solver.solvers;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.TypeBoundMapperApplier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeBoundMapperSolver implements TypeSolver {
    private String name;
    private List<TypeBoundMapperApplier> appliers;
    private Set<Class<? extends TypeBound>> supported;
    private TypeBound.Classification classification = TypeBound.Classification.CONSTRAINT;

    private Set<TypeBound> constraints = new LinkedHashSet<>();

    public TypeBoundMapperSolver(String name, Set<Class<? extends TypeBound>> supported, List<TypeBoundMapperApplier> appliers) {
        this.name = name;
        this.appliers = appliers;
        this.supported = supported;
    }

    public TypeBoundMapperSolver(String name, Set<Class<? extends TypeBound>> supported, TypeBoundMapperApplier... appliers) {
        this(name, supported, List.of(appliers));
    }

    public TypeBoundMapperSolver setClassification(TypeBound.Classification classification) {
        this.classification = classification;
        return this;
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

    @Override
    public Result solve(TypeSystem system) {
        List<TypeBound.Result.Builder> building = this.constraints.stream().map(TypeBound.Result::builder).collect(Collectors.toCollection(ArrayList::new));

        List<TypeBound.Result.Builder> constraints;
        List<TypeBound.Result.Builder> bounds;

        if (this.classification == TypeBound.Classification.BOUND) {
            constraints = new ArrayList<>();
            bounds = new ArrayList<>(building);
        } else {
            constraints = new ArrayList<>(building);
            bounds = new ArrayList<>();
        }

        for (TypeBoundMapperApplier applier : this.appliers) {
            Pair<List<TypeBound.Result.Builder>, List<TypeBound.Result.Builder>> result = applier.process(system, bounds, constraints);
            bounds = result.left();
            constraints = result.right();
        }

        return new Result(building.stream().map(TypeBound.Result.Builder::build).collect(Collectors.toCollection(LinkedHashSet::new)));
    }
}
