package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;

public class EqualType implements UnaryTypeBoundMapper<TypeBound.Equal> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.bound() instanceof TypeBound.Equal;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder constraint, TypeBound.Equal bound) {
        context.bounds().accept(constraint.setSatisfied(bound.left().typeEquals(bound.right())));
    }
}
