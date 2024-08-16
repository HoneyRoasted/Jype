package honeyroasted.jype.system.solver.solvers;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.TypeBoundMapperApplier;
import honeyroasted.jype.type.Type;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TypeBoundMapperSolver extends AbstractTypeSolver {

    private List<TypeBoundMapperApplier> appliers;
    private List<Function<Type, Type>> typeModifiers;
    private TypeBound.Classification classification;


    public TypeBoundMapperSolver(String name, TypeBound.Classification classification, Set<Class<? extends TypeBound>> supported, List<Function<Type, Type>> typeModifiers, List<TypeBoundMapperApplier> appliers) {
        super(name, supported);
        this.appliers = appliers;
        this.classification = classification;
        this.typeModifiers = typeModifiers;
    }

    public TypeBoundMapperSolver(String name, TypeBound.Classification classification, Set<Class<? extends TypeBound>> supported, TypeBoundMapperApplier... appliers) {
        this(name, classification, supported, new ArrayList<>(), List.of(appliers));
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
            Pair<List<TypeBound.Result.Builder>, List<TypeBound.Result.Builder>> result = applier.process(system, new ArrayList<>(), bounds, constraints);
            bounds = result.left();
            constraints = result.right();
        }

        return new Result(building.stream().map(TypeBound.Result.Builder::build).collect(Collectors.toCollection(LinkedHashSet::new)));
    }
}
