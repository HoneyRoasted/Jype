package honeyroasted.jype.system.solver.inference;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.AbstractTypeSolver;
import honeyroasted.jype.system.solver.TypeConstraint;
import honeyroasted.jype.system.solver.TypeSolution;

public class InferenceTypeSolver extends AbstractTypeSolver {
    
    public InferenceTypeSolver(TypeSystem system) {
        super(system,
                TypeConstraint.Bound.class, TypeConstraint.Equal.class,
                TypeConstraint.True.class, TypeConstraint.False.class);
    }

    @Override
    public TypeSolution solve() {
        return null;
    }
}
