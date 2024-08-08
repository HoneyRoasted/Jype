package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.type.MetaVarType;

public class UpdateMetaVars implements TypeOperation<TypeBound.Result.Builder, TypeBound.Result.Builder> {
    @Override
    public TypeBound.Result.Builder apply(TypeSystem system, TypeBound.Result.Builder builder) {
        TypeBound bound = builder.bound();
        if (bound instanceof TypeBound.Equal eq) {
            if (eq.left() instanceof MetaVarType mvt) {
                mvt.equalities().add(eq.right());
            }

            if (eq.right() instanceof MetaVarType mvt) {
                mvt.equalities().add(eq.left());
            }
        }

        if (bound instanceof TypeBound.Subtype st) {
            if (st.left() instanceof MetaVarType mvt) {
                mvt.upperBounds().add(st.right());
            }

            if (st.right() instanceof MetaVarType mvt) {
                mvt.lowerBounds().add(st.right());
            }
        }

        return builder;
    }
}
