package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

import java.util.Set;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class SubtypeMetaVar implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.Subtype st &&
                (st.left() instanceof MetaVarType || st.right() instanceof MetaVarType);
    }

    @Override
    public void map(Set<TypeBound.Result.Builder> results, TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        Type subtype = bound.left();
        Type supertype = bound.right();
        if (subtype instanceof MetaVarType mvt) {
            if (mvt.upperBounds().isEmpty()) {
                results.add(constraint.setSatisfied(false));
            } else {
                constraint.setPropagation(TypeBound.Result.Propagation.OR);
                mvt.upperBounds().forEach(t -> results.add(TypeBound.Result.builder(new TypeBound.Subtype(t, supertype), constraint)));
            }
        } else if (supertype instanceof MetaVarType mvt) {
            if (mvt.lowerBounds().isEmpty()) {
                results.add(constraint.setSatisfied(false));
            } else {
                constraint.setPropagation(TypeBound.Result.Propagation.AND);
                mvt.lowerBounds().forEach(t -> results.add(TypeBound.Result.builder(new TypeBound.Subtype(subtype, t), constraint)));
            }
        }
    }
}
