package honeyroasted.jype.system.solver.solvers;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.bounds.TypeBound;

import java.util.LinkedHashSet;
import java.util.Set;

public class InferenceTypeSolver implements TypeSolver {
    private static Set<Class<? extends TypeBound>> accepted = Set.of(
            TypeBound.Infer.class,

            TypeBound.Equal.class,
            TypeBound.Compatible.class,
            TypeBound.ExpressionCompatible.class,
            TypeBound.Contains.class,
            TypeBound.LambdaThrows.class,
            TypeBound.Throws.class,
            TypeBound.Capture.class,
            TypeBound.Subtype.class
    );

    private Set<TypeBound> constraints = new LinkedHashSet<>();

    @Override
    public boolean supports(TypeBound bound) {
        return accepted.stream().anyMatch(c -> c.isInstance(bound));
    }

    @Override
    public TypeSolver bind(TypeBound bound) {
        this.constraints.add(bound);
        return this;
    }

    @Override
    public void reset() {
        this.constraints.clear();
    }

    @Override
    public Result solve(TypeSystem system) {
        return null;
    }
}
