package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.PrimitiveType;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Compatible.Context.*;

public class CompatibleLooseInvocation implements UnaryTypeBoundMapper<TypeBound.Compatible> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.Compatible bound) {
        return constraint.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN && (bound.context() == LOOSE_INVOCATION || bound.context() == ASSIGNMENT);
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Compatible bound) {
        builder.setPropagation(TypeBound.Result.Propagation.OR);
        context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), bound.right()), builder));

        if (bound.left() instanceof PrimitiveType l && !(bound.right() instanceof PrimitiveType)) {
            context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(l.box(), bound.right()), builder));
        } else if (!(bound.left() instanceof PrimitiveType) && bound.right() instanceof PrimitiveType r) {
            context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), r.box()), builder));
        }
    }
}
