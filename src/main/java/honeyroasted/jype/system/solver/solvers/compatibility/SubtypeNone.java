package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.PrimitiveType;

import java.util.Set;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class SubtypeNone implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.Subtype st &&
                (st.left() instanceof NoneType || st.right() instanceof NoneType);
    }

    @Override
    public void map(Set<TypeBound.Result.Builder> results, TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        if (bound.right() instanceof NoneType) {
            results.add(constraint.setSatisfied(false));
        } else if (bound.left() instanceof NoneType l) {
            results.add(constraint.setSatisfied(l.isNullType() && !(bound.left() instanceof PrimitiveType)));
        }
    }
}