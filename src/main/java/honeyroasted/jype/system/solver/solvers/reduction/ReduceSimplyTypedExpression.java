package honeyroasted.jype.system.solver.solvers.reduction;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;

public class ReduceSimplyTypedExpression implements UnaryTypeBoundMapper<TypeBound.ExpressionCompatible> {
    @Override
    public boolean accepts(TypeBound.Classification classification) {
        return classification == TypeBound.Classification.CONSTRAINT;
    }

    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.ExpressionCompatible bound) {
        return bound.left().isSimplyTyped();
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.ExpressionCompatible bound) {
        builder.setPropagation(TypeBound.Result.Propagation.INHERIT);
        context.constraints().accept(TypeBound.Result.builder(new TypeBound.Compatible(bound.left().getSimpleType(bound.right().typeSystem()).get(), bound.right(), bound.context()), builder));
    }
}
