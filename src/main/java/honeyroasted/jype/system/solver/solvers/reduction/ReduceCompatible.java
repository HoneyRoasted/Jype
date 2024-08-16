package honeyroasted.jype.system.solver.solvers.reduction;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

public class ReduceCompatible implements UnaryTypeBoundMapper<TypeBound.Compatible> {
    @Override
    public boolean accepts(TypeBound.Classification classification) {
        return classification == TypeBound.Classification.CONSTRAINT;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Compatible bound) {
        builder.setPropagation(TypeBound.Result.Propagation.AND);

        Type left = context.view(bound.left());
        Type right = context.view(bound.right());

        if (left.isProperType() && right.isProperType()) {
            context.bounds().accept(context.system().operations().compatibilityApplier()
                    .apply(context.system(), bound, TypeBound.Classification.BOUND, builder));
        } else if (left instanceof PrimitiveType pt) {
            context.constraints().accept(TypeBound.Result.builder(new TypeBound.Compatible(pt.box(), right, bound.context()), builder));
        } else if (right instanceof PrimitiveType pt) {
            context.constraints().accept(TypeBound.Result.builder(new TypeBound.Equal(left, pt.box()), builder));
        } else if (left instanceof ClassType pct && pct.hasAnyTypeArguments() && right instanceof ClassType ct && !ct.hasAnyTypeArguments() &&
                context.system().operations().compatibilityApplier().check(context.system(), new TypeBound.Subtype(pct.classReference(), ct.classReference()), TypeBound.Classification.BOUND, builder)) {
            context.bounds().accept(builder.setSatisfied(true));
        } else if (left instanceof ArrayType at && at.deepComponent() instanceof ClassType pct && pct.hasAnyTypeArguments() &&
                right instanceof ArrayType rat && rat.deepComponent() instanceof ClassType rpct && !rpct.hasAnyTypeArguments() &&
                at.depth() == rat.depth() &&
                context.system().operations().compatibilityApplier().check(context.system(), new TypeBound.Subtype(pct.classReference(), rpct.classReference()), TypeBound.Classification.BOUND, builder)) {
            context.bounds().accept(builder.setSatisfied(true));
        } else {
            context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(left, right), builder));
        }
    }
}
