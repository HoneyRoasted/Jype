package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.PrimitiveType;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Compatible.Context.*;
import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class CompatibleLooseInvocation implements UnaryTypeBoundMapper<TypeBound.Compatible> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.Compatible cmpt &&
                (cmpt.context() == LOOSE_INVOCATION || cmpt.context() == ASSIGNMENT);
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder constraint, TypeBound.Compatible bound) {
        constraint.setPropagation(TypeBound.Result.Propagation.OR);
        context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), bound.right()), constraint));

        if (bound.left() instanceof PrimitiveType l && !(bound.right() instanceof PrimitiveType)) {
            context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(l.box(), bound.right()), constraint));
        } else if (!(bound.left() instanceof PrimitiveType) && bound.right() instanceof PrimitiveType r) {
            context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), r.box()), constraint));
        }
    }
}
