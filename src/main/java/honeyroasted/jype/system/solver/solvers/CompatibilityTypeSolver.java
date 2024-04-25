package honeyroasted.jype.system.solver.solvers;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.solvers.inference.helper.TypeCompatibilityChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CompatibilityTypeSolver extends AbstractTypeSolver {
    private TypeCompatibilityChecker compatibilityChecker;

    public CompatibilityTypeSolver() {
        super(Set.of(TypeBound.Equal.class,
                TypeBound.Subtype.class,
                TypeBound.Compatible.class));
        this.compatibilityChecker = new TypeCompatibilityChecker(this);
    }

    @Override
    public void reset() {
        this.initialBounds.clear();
    }

    @Override
    public Result solve(TypeSystem system) {
        List<TypeBound.Result.Builder> results = new ArrayList<>();

        for (TypeBound bound : this.initialBounds) {
            if (bound instanceof TypeBound.Subtype st) {
                results.add(this.compatibilityChecker.check(st));
            } else if (bound instanceof TypeBound.Compatible ct) {
                results.add(this.compatibilityChecker.check(ct));
            } else if (bound instanceof TypeBound.Equal eq) {
                results.add(TypeBound.Result.builder(eq)
                        .setSatisfied(eq.left().typeEquals(eq.right())));
            }
        }

        return new Result(results.stream().map(TypeBound.Result.Builder::build).collect(Collectors.toSet()));
    }
}
