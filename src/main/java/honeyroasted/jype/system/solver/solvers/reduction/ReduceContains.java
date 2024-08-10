package honeyroasted.jype.system.solver.solvers.reduction;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;

public class ReduceContains implements UnaryTypeBoundMapper<TypeBound.Contains> {
    @Override
    public boolean accepts(TypeBound.Classification classification) {
        return classification == TypeBound.Classification.CONSTRAINT;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Contains bound) {
        Type s = bound.left();
        Type t = bound.right();

        if (t instanceof WildType) {
            if (t instanceof WildType.Upper wtu) {
                if (wtu.hasDefaultBounds()) {
                    builder.setSatisfied(true);
                } else {
                    if (s instanceof WildType) {
                        if (s instanceof WildType.Upper swtu) {
                            if (swtu.hasDefaultBounds()) {
                                context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(s.typeSystem().constants().object(), wtu.upperBound()), builder));
                            } else {
                                context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(swtu.upperBound(), wtu.upperBound()), builder));
                            }
                        } else if (s instanceof WildType.Lower swtl) {
                            context.constraints().accept(TypeBound.Result.builder(new TypeBound.Equal(s.typeSystem().constants().object(), wtu.upperBound()), builder));
                        }
                    } else {
                        context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(s, wtu.upperBound()), builder));
                    }
                }
            } else if (t instanceof WildType.Lower wtl) {
                if (s instanceof WildType) {
                    if (s instanceof WildType.Lower swtl) {
                        context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(wtl.lowerBound(), swtl.lowerBound()), builder));
                    } else {
                        builder.setSatisfied(false);
                    }
                } else {
                    context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(wtl.lowerBound(), s), builder));
                }
            }
        } else {
            if (s instanceof WildType) {
                builder.setSatisfied(false);
            } else {
                context.constraints().accept(TypeBound.Result.builder(new TypeBound.Equal(s, t), builder));
            }
        }
    }
}
