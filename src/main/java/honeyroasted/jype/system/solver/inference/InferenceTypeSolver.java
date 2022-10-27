package honeyroasted.jype.system.solver.inference;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.AbstractTypeSolver;
import honeyroasted.jype.system.solver.TypeConstraint;
import honeyroasted.jype.system.solver.TypeSolution;

public class InferenceTypeSolver extends AbstractTypeSolver {

    public InferenceTypeSolver(TypeSystem system) {
        super(system,
                ConstraintFormula.TypeCompatible.class, ConstraintFormula.Subtype.class,
                ConstraintFormula.ExpressionCompatible.class, ConstraintFormula.Contained.class,
                ConstraintFormula.Equal.class, ConstraintFormula.LambdaThrows.class,
                ConstraintFormula.MethodRefThrows.class,

                TypeBound.Equal.class, TypeBound.LowerBound.class,
                TypeBound.UpperBound.class, TypeBound.False.class,
                TypeBound.Capture.class, TypeBound.Throws.class);
    }

    @Override
    public TypeSolution solve() {
        return null;
    }
}
