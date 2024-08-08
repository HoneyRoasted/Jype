package honeyroasted.jype.system.solver.solvers.reduction;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;

public class ReduceExpressionCompatible implements UnaryTypeBoundMapper<TypeBound.ExpressionCompatible> {
    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.ExpressionCompatible bound) {
        if (bound.left().isSimplyTyped()) {
            builder.setPropagation(TypeBound.Result.Propagation.INHERIT);
            context.constraints().accept(TypeBound.Result.builder(new TypeBound.Compatible(bound.left().getSimpleType(bound.right().typeSystem()).get(), bound.right(), bound.context()), builder));
        } else {
            //TODO
            builder.setSatisfied(false);
        }
    }
}
