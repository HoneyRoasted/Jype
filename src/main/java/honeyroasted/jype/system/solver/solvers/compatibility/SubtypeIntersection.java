package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.IntersectionType;

public class SubtypeIntersection implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        return constraint.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN && bound.left() instanceof IntersectionType || bound.right() instanceof IntersectionType;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Subtype bound) {
        if (bound.left() instanceof IntersectionType l) {
            builder.setPropagation(TypeBound.Result.Propagation.OR);
            l.children().forEach(t -> context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(t, bound.right()), builder)));
        } else if (bound.right() instanceof IntersectionType r) {
            builder.setPropagation(TypeBound.Result.Propagation.AND);
            r.children().forEach(t -> context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), t), builder)));
        }
    }
}
