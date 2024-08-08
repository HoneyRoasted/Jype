package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Compatible.Context.*;

public class CompatibleExplicitCast implements UnaryTypeBoundMapper<TypeBound.Compatible> {

    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.Compatible bound) {
        return constraint.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN && bound.context() == EXPLICIT_CAST;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Compatible bound) {
        builder.setPropagation(TypeBound.Result.Propagation.OR);
        addAll(context.constraints(),
                TypeBound.Result.builder(new TypeBound.Compatible(bound.left(), bound.right(), LOOSE_INVOCATION), builder),
                TypeBound.Result.builder(new TypeBound.Compatible(bound.right(), bound.left(), LOOSE_INVOCATION), builder));
    }
}
