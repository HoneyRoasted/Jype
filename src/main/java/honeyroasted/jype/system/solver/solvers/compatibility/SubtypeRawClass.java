package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.ClassType;

public class SubtypeRawClass implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        return constraint.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN &&
                bound.left() instanceof ClassType l && !l.hasAnyTypeArguments() &&
                bound.right() instanceof ClassType r && !r.hasAnyTypeArguments();
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Subtype bound) {
        ClassType l = (ClassType) bound.left();
        ClassType r = (ClassType) bound.right();

        if (l.hasRelevantOuterType() || r.hasRelevantOuterType()) {
            if (l.hasAnyTypeArguments() && r.hasAnyTypeArguments()) {
                context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(l.outerType(), r.outerType()), builder));
            } else {
                context.bounds().accept(builder.setSatisfied(false));
            }
        } else {
            context.bounds().accept(builder.setSatisfied(l.classReference().hasSupertype(r.classReference())));
        }
    }
}
