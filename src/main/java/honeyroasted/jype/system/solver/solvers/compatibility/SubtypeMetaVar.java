package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

public class SubtypeMetaVar implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        return constraint.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN && bound.left() instanceof MetaVarType || bound.right() instanceof MetaVarType;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Subtype bound) {
        Type subtype = bound.left();
        Type supertype = bound.right();
        if (subtype instanceof MetaVarType mvt) {
            if (mvt.upperBounds().isEmpty()) {
                context.bounds().accept(builder.setSatisfied(false));
            } else {
                builder.setPropagation(TypeBound.Result.Propagation.OR);
                mvt.upperBounds().forEach(t -> context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(t, supertype), builder)));
            }
        } else if (supertype instanceof MetaVarType mvt) {
            if (mvt.lowerBounds().isEmpty()) {
                context.bounds().accept(builder.setSatisfied(false));
            } else {
                builder.setPropagation(TypeBound.Result.Propagation.AND);
                mvt.lowerBounds().forEach(t -> context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(subtype, t), builder)));
            }
        }
    }
}
