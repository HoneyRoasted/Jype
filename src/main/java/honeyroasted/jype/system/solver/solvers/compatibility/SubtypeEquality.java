package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.Type;

public class SubtypeEquality implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        return constraint.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Subtype bound) {
        Type left = context.view(bound.left());
        Type right = context.view(bound.right());
        if (left.typeEquals(right)) {
            builder.setSatisfied(true);
            context.bounds().accept(TypeBound.Result.builder(new TypeBound.Equal(left, right), builder).setSatisfied(true));
        }
    }
}
