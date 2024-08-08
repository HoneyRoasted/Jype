package honeyroasted.jype.system.solver.solvers.compatibility;

import honeyroasted.jype.system.TypeConstants;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.UnaryTypeBoundMapper;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

import static honeyroasted.jype.system.solver.bounds.TypeBound.Result.Trinary.*;

public class SubtypeArray implements UnaryTypeBoundMapper<TypeBound.Subtype> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.getSatisfied() == UNKNOWN && constraint.bound() instanceof TypeBound.Subtype st &&
                st.left() instanceof ArrayType;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder constraint, TypeBound.Subtype bound) {
        ArrayType l = (ArrayType) bound.left();
        Type supertype = bound.right();
        if (supertype instanceof ArrayType r) {
            if (l.component() instanceof PrimitiveType || r.component() instanceof PrimitiveType) {
                context.constraints().accept(constraint.setSatisfied(r.component().typeEquals(l.component())));
            } else {
                context.constraints().accept(TypeBound.Result.builder(new TypeBound.Subtype(l.component(), r.component()), constraint));
            }
        } else {
            TypeConstants c = supertype.typeSystem().constants();
            constraint.setPropagation(TypeBound.Result.Propagation.OR);
            addAll(context.constraints(),
                    TypeBound.Result.builder(new TypeBound.Subtype(c.object(), supertype), constraint),
                    TypeBound.Result.builder(new TypeBound.Subtype(c.cloneable(), supertype), constraint),
                    TypeBound.Result.builder(new TypeBound.Subtype(c.serializable(), supertype), constraint)
            );
        }
    }
}
