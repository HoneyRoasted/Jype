package honeyroasted.jype.system.solver.solvers.reduction;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

public class ReduceEqual implements UnaryTypeBoundMapper<TypeBound.Equal> {

    @Override
    public TypeBound.Classification classification() {
        return TypeBound.Classification.CONSTRAINT;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Equal bound) {
        Type s = bound.left();
        Type t = bound.right();

        if (s.isProperType() && t.isProperType()) {
            builder.setSatisfied(s.typeEquals(t));
        } else if (s.isNullType() || t.isNullType()) {
            builder.setSatisfied(false);
        } else if (s instanceof MetaVarType && !(t instanceof PrimitiveType)) {
            context.bounds().accept(TypeBound.Result.builder(new TypeBound.Equal(s, t), builder));
        } else if (t instanceof MetaVarType && !(s instanceof PrimitiveType)) {
            context.bounds().accept(TypeBound.Result.builder(new TypeBound.Equal(t, s), builder));
        }
    }
}
