package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Compatible.Context.*;

public class CompatibleStrictInvocation implements UnaryTypeBoundMapper<TypeBound.Compatible> {

    @Override
    public boolean accepts(TypeBound.Result.Builder constraint, TypeBound.Compatible bound) {
        return constraint.getSatisfied() == TypeBound.Result.Trinary.UNKNOWN && bound.context() == STRICT_INVOCATION;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Compatible bound) {
        context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), bound.right()), builder));
    }
}
