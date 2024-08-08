package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.VarType;

public class SubtypeVar implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        return constraint.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN && bound.left() instanceof VarType;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Subtype bound) {
        VarType l = (VarType) bound.left();
        builder.setPropagation(TypeBound.Result.Propagation.OR);
        l.upperBounds().forEach(t -> context.defaultConsumer().accept(TypeBound.Result.builder(new TypeBound.Subtype(t, bound.right()), builder)));
    }
}
