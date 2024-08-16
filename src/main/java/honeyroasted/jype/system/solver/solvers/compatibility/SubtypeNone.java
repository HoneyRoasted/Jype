package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

public class SubtypeNone implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        return constraint.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN && bound.left() instanceof NoneType || bound.right() instanceof NoneType;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Subtype bound) {
        Type left = context.view(bound.left());
        Type right = context.view(bound.right());

        if (right instanceof NoneType) {
            context.bounds().accept(builder.setSatisfied(false));
        } else if (left instanceof NoneType l) {
            context.bounds().accept(builder.setSatisfied(l.isNullType() && !(right instanceof PrimitiveType)));
        }
    }
}
