package honeyroasted.jype.system.solver.inference;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.AbstractTypeSolver;
import honeyroasted.jype.system.solver.TypeSolution;
import honeyroasted.jype.system.solver.inference.model.adapter.ExpressionAdapter;

import java.util.List;

public class InferenceTypeSolver extends AbstractTypeSolver {
    private ExpressionAdapter adapter;

    public InferenceTypeSolver(TypeSystem system, ExpressionAdapter adapter) {
        super(system,
                ConstraintFormula.TypeCompatible.class, ConstraintFormula.Subtype.class,
                ConstraintFormula.ExpressionCompatible.class, ConstraintFormula.Contained.class,
                ConstraintFormula.Equal.class, ConstraintFormula.LambdaThrows.class,
                ConstraintFormula.MethodRefThrows.class,

                TypeBound.Equal.class, TypeBound.LowerBound.class,
                TypeBound.UpperBound.class, TypeBound.False.class,
                TypeBound.Capture.class, TypeBound.Throws.class);
        this.adapter = adapter;
    }

    private List<ConstraintFormula> constraintFormulas;
    private List<TypeBound> typeBounds;

    public InferenceTypeSolver copy() {
        InferenceTypeSolver copy = new InferenceTypeSolver(this.system, this.adapter);
        this.constraints.forEach(copy::constrain);
        copy.setConstraintFormulas(this.constraintFormulas);
        copy.setTypeBounds(this.typeBounds);
        return copy;
    }

    public List<ConstraintFormula> constraintFormulas() {
        return this.constraintFormulas;
    }

    public void setConstraintFormulas(List<ConstraintFormula> constraintFormulas) {
        this.constraintFormulas = List.copyOf(constraintFormulas);
    }

    public List<TypeBound> typeBounds() {
        return this.typeBounds;
    }

    public void setTypeBounds(List<TypeBound> typeBounds) {
        this.typeBounds = List.copyOf(typeBounds);
    }

    public void collectConstraints() {
        this.constraintFormulas = this.constraints.stream().filter(t -> t instanceof ConstraintFormula).map(t -> (ConstraintFormula) t).toList();
        this.typeBounds = this.constraints.stream().filter(t -> t instanceof TypeBound).map(t -> (TypeBound) t).toList();
    }

    public void performReduction() {

    }

    public void performIncorporation() {

    }

    public TypeSolution performResolution() {
        return null;
    }

    @Override
    public TypeSolution solve() {
        this.collectConstraints();
        this.performReduction();
        this.performIncorporation();
        return this.performResolution();
    }
}
