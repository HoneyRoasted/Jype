package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.PrimitiveType;

public class SubtypeNone implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        return constraint.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN && bound.left() instanceof NoneType || bound.right() instanceof NoneType;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Subtype bound) {
        if (bound.right() instanceof NoneType) {
            context.bounds().accept(builder.setSatisfied(false));
        } else if (bound.left() instanceof NoneType l) {
            context.bounds().accept(builder.setSatisfied(l.isNullType() && !(bound.left() instanceof PrimitiveType)));
        }
    }
}
