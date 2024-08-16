package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.ClassType;

public class SubtypeUnchecked implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        return constraint.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN &&
                bound.left() instanceof ClassType l && bound.right() instanceof ClassType r &&
                ((l.hasAnyTypeArguments() && !r.hasAnyTypeArguments()) ||
                        (!l.hasAnyTypeArguments() && r.hasAnyTypeArguments()));
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Subtype bound) {
        ClassType l = context.view(bound.left());
        ClassType r = context.view(bound.right());

        context.bounds().accept(TypeBound.Result.builder(new TypeBound.Subtype(l.classReference(), r.classReference()), builder)
                .setSatisfied(l.classReference().hasSupertype(r.classReference())));
    }
}
