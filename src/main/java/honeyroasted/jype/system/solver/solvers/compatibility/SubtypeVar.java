package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.VarType;

import java.util.List;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class SubtypeVar implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.Subtype st &&
                st.left() instanceof VarType;
    }

    @Override
    public void map(List<TypeBound.Result.Builder> bounds, List<TypeBound.Result.Builder> constraints, TypeBound.Classification classification, TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        List<TypeBound.Result.Builder> results = classification == TypeBound.Classification.BOUND ? bounds : constraints;
        VarType l = (VarType) bound.left();
        constraint.setPropagation(TypeBound.Result.Propagation.OR);
        l.upperBounds().forEach(t -> results.add(TypeBound.Result.builder(new TypeBound.Subtype(t, bound.right()), constraint)));
    }
}
