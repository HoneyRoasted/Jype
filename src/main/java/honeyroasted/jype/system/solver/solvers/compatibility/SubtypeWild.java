package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.WildType;

public class SubtypeWild implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        return constraint.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN &&
                (bound.left() instanceof WildType.Upper || bound.right() instanceof WildType.Lower);
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Subtype bound) {
        if (bound.left() instanceof WildType.Upper l) {
            builder.setPropagation(TypeBound.Result.Propagation.OR);
            l.upperBounds().forEach(t -> context.defaultConsumer().accept(TypeBound.Result.builder(new TypeBound.Subtype(t, bound.right()), builder)));
        } else if (bound.right() instanceof WildType.Lower r) {
            builder.setPropagation(TypeBound.Result.Propagation.AND);
            r.lowerBounds().forEach(t -> context.defaultConsumer().accept(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), t), builder)));
        }
    }
}
