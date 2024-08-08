package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.expression.ExpressionInformation;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.Type;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Compatible.Context.*;

public class ExpressionSimplyTyped implements UnaryTypeBoundMapper<TypeBound.ExpressionCompatible> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.ExpressionCompatible bound) {
        return constraint.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN && bound.left().isSimplyTyped() && !(bound.context() == ASSIGNMENT && bound.left() instanceof ExpressionInformation.Constant);
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.ExpressionCompatible bound) {
        Type supertype = bound.right();
        Type expr = bound.left().getSimpleType(supertype.typeSystem()).get();

        context.constraints().accept(TypeBound.Result.builder(new TypeBound.Compatible(expr, supertype, bound.context()), builder));
    }
}
