package honeyroasted.jype.system.solver.solvers.inference;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.solvers.AbstractTypeSolver;
import honeyroasted.jype.type.delegate.AbstractTypeDelegate;

import java.util.Set;

public class InferenceTypeSolver extends AbstractTypeSolver {
    private ExpressionResolver expressionResolver;

    public InferenceTypeSolver(ExpressionResolver expressionResolver) {
        super(Set.of(TypeBound.False.class, TypeBound.Equal.class,
                        TypeBound.Compatible.class, TypeBound.ExpressionCompatible.class,
                        TypeBound.Contains.class, TypeBound.LambdaThrows.class,
                        TypeBound.Throws.class, TypeBound.Captures.class,
                        TypeBound.Subtype.class),
                Set.of(TypeBound.NeedsInference.class));
        this.expressionResolver = expressionResolver;
    }

    @Override
    public Result solve(TypeSystem system) {
        return null;
    }

}
