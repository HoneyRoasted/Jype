package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;

public class SubtypeEquality implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        return constraint.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN && bound.left().typeEquals(bound.right());
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Subtype bound) {
        builder.setSatisfied(true);
        context.bounds().accept(TypeBound.Result.builder(new TypeBound.Equal(bound.left(), bound.right()), builder).setSatisfied(true));
    }
}
