package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.ClassType;

import java.util.List;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class SubtypeUnchecked implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.Subtype st &&
                st.left() instanceof ClassType l && st.right() instanceof ClassType r &&
                ((l.hasAnyTypeArguments() && !r.hasTypeArguments()) ||
                        (!l.hasAnyTypeArguments() && r.hasTypeArguments()));
    }

    @Override
    public void map(List<TypeBound.Result.Builder> results, TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        ClassType l = (ClassType) bound.left();
        ClassType r = (ClassType) bound.right();

        results.add(TypeBound.Result.builder(new TypeBound.Subtype(l.classReference(), r.classReference()), constraint)
                .setSatisfied(l.classReference().hasSupertype(r.classReference())));
    }
}
