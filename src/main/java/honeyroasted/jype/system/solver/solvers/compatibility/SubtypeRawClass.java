package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.ClassType;

import java.util.List;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class SubtypeRawClass implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.Subtype st &&
                st.left() instanceof ClassType l && !l.hasAnyTypeArguments() &&
                st.right() instanceof ClassType r && !r.hasAnyTypeArguments();
    }

    @Override
    public void map(List<TypeBound.Result.Builder> bounds, List<TypeBound.Result.Builder> constraints, TypeBound.Classification classification, TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        ClassType l = (ClassType) bound.left();
        ClassType r = (ClassType) bound.right();

        if (l.hasRelevantOuterType() || r.hasRelevantOuterType()) {
            if (l.hasAnyTypeArguments() && r.hasAnyTypeArguments()) {
                constraints.add(TypeBound.Result.builder(new TypeBound.Subtype(l.outerType(), r.outerType()), constraint));
            } else {
                bounds.add(constraint.setSatisfied(false));
            }
        } else {
            bounds.add(constraint.setSatisfied(l.classReference().hasSupertype(r.classReference())));
        }
    }
}
