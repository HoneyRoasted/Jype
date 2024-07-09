package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.ClassType;

import java.util.Set;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class SubtypeRawClass implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.Subtype st &&
                st.left() instanceof ClassType l && !l.hasAnyTypeArguments() &&
                st.right() instanceof ClassType r && !r.hasAnyTypeArguments();
    }

    @Override
    public void map(Set<TypeBound.Result.Builder> results, TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        ClassType l = (ClassType) bound.left();
        ClassType r = (ClassType) bound.right();

        if (l.hasRelevantOuterType() || r.hasRelevantOuterType()) {
            if (l.hasAnyTypeArguments() && r.hasRelevantOuterType()) {
                results.add(TypeBound.Result.builder(new TypeBound.Subtype(l.outerType(), r.outerType()), constraint));
            } else {
                results.add(constraint.setSatisfied(false));
            }
        } else {
            results.add(constraint.setSatisfied(l.classReference().hasSupertype(r.classReference())));
        }
    }
}
