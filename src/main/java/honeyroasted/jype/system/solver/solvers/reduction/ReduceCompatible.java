package honeyroasted.jype.system.solver.solvers.reduction;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.PrimitiveType;

public class ReduceCompatible implements UnaryTypeBoundMapper<TypeBound.Compatible> {
    @Override
    public boolean accepts(TypeBound.Classification classification) {
        return classification == TypeBound.Classification.CONSTRAINT;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Compatible bound) {
        builder.setPropagation(TypeBound.Result.Propagation.AND);

        if (bound.left().isProperType() && bound.right().isProperType()) {
            context.bounds().accept(context.system().operations().compatibilityApplier()
                    .apply(context.system(), bound, TypeBound.Classification.BOUND, builder));
        } else if (bound.left() instanceof PrimitiveType pt) {
            context.constraints().accept(TypeBound.Result.builder(new TypeBound.Compatible(pt.box(), bound.right(), bound.context()), builder));
        } else if (bound.right() instanceof PrimitiveType pt) {
            context.constraints().accept(TypeBound.Result.builder(new TypeBound.Equal(bound.left(), pt.box()), builder));
        } else if (bound.left() instanceof ClassType pct && pct.hasAnyTypeArguments() && bound.right() instanceof ClassType ct && !ct.hasAnyTypeArguments() &&
                context.system().operations().compatibilityApplier().check(context.system(), new TypeBound.Subtype(pct.classReference(), ct.classReference()), TypeBound.Classification.BOUND, builder)) {
            context.bounds().accept(builder.setSatisfied(true));
        } else if (bound.left() instanceof ArrayType at && at.deepComponent() instanceof ClassType pct && pct.hasAnyTypeArguments() &&
                bound.right() instanceof ArrayType rat && rat.deepComponent() instanceof ClassType rpct && !rpct.hasAnyTypeArguments() &&
                at.depth() == rat.depth() &&
                context.system().operations().compatibilityApplier().check(context.system(), new TypeBound.Subtype(pct.classReference(), rpct.classReference()), TypeBound.Classification.BOUND, builder)) {
            context.bounds().accept(builder.setSatisfied(true));
        } else {
            context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), bound.right()), builder));
        }
    }
}
