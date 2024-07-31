package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;

import java.util.List;

public class EqualType implements UnaryTypeBoundMapper<TypeBound.Equal> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.bound() instanceof TypeBound.Equal;
    }

    @Override
    public void map(List<TypeBound.Result.Builder> bounds, List<TypeBound.Result.Builder> constraints, TypeBound.Classification classification, TypeBound.Result.Builder constraint, TypeBound.Equal bound) {
        bounds.add(constraint.setSatisfied(bound.left().typeEquals(bound.right())));
    }
}
