package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.ClassType;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class SubtypeRawClass implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.Subtype st &&
                st.left() instanceof ClassType l && !l.hasAnyTypeArguments() &&
                st.right() instanceof ClassType r && !r.hasAnyTypeArguments();
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        ClassType l = (ClassType) bound.left();
        ClassType r = (ClassType) bound.right();

        if (l.hasRelevantOuterType() || r.hasRelevantOuterType()) {
            if (l.hasAnyTypeArguments() && r.hasAnyTypeArguments()) {
                context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(l.outerType(), r.outerType()), constraint));
            } else {
                context.bounds().accept(constraint.setSatisfied(false));
            }
        } else {
            context.bounds().accept(constraint.setSatisfied(l.classReference().hasSupertype(r.classReference())));
        }
    }
}
