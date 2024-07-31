package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.WildType;

import java.util.List;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class SubtypeWild implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.Subtype st &&
                (st.left() instanceof WildType.Upper | st.right() instanceof WildType.Lower);
    }

    @Override
    public void map(List<TypeBound.Result.Builder> bounds, List<TypeBound.Result.Builder> constraints, TypeBound.Classification classification, TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        List<TypeBound.Result.Builder> results = classification == TypeBound.Classification.BOUND ? bounds : constraints;
        if (bound.left() instanceof WildType.Upper l) {
            constraint.setPropagation(TypeBound.Result.Propagation.OR);
            l.upperBounds().forEach(t -> results.add(TypeBound.Result.builder(new TypeBound.Subtype(t, bound.right()), constraint)));
        } else if (bound.right() instanceof WildType.Lower r) {
            constraint.setPropagation(TypeBound.Result.Propagation.AND);
            r.lowerBounds().forEach(t -> results.add(TypeBound.Result.builder(new TypeBound.Subtype(bound.left(), t), constraint)));
        }
    }
}
