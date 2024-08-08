package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class SubtypeEquality implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.Subtype st &&
                st.left().typeEquals(st.right());
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        constraint.setSatisfied(true);
        context.bounds().accept(TypeBound.Result.builder(new TypeBound.Equal(bound.left(), bound.right()), constraint).setSatisfied(true));
    }
}
