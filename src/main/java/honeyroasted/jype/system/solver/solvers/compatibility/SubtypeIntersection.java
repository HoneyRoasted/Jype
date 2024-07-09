package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.IntersectionType;

import java.util.Set;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class SubtypeIntersection implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.Subtype st &&
                (st.left() instanceof IntersectionType || st.right() instanceof IntersectionType);
    }

    @Override
    public void map(Set<TypeBound.Result.Builder> results, TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        if (bound.left() instanceof IntersectionType l) {
            constraint.setPropagation(TypeBound.Result.Propagation.OR);
            l.children().forEach(t -> results.add(TypeBound.Result.builder(new TypeBound.Subtype(t, bound.right()), constraint)));
        } else if (bound.right() instanceof IntersectionType r) {
            constraint.setPropagation(TypeBound.Result.Propagation.AND);
            r.children().forEach(t -> results.add(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), t), constraint)));
        }
    }
}
