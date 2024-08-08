package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;

public class EqualType implements UnaryTypeBoundMapper<TypeBound.Equal> {

    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.Equal bound) {
        return constraint.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Equal bound) {
        context.bounds().accept(builder.setSatisfied(bound.left().typeEquals(bound.right())));
    }
}
