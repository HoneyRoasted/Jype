package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.TypeBoundMapper;
import honeyroasted.jype.type.MetaVarType;

import java.util.Collections;
import java.util.Set;

public class UpdateMetaVars implements TypeBoundMapper {
    @Override
    public int arity() {
        return -1;
    }

    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return true;
    }

    @Override
    public void map(Set<TypeBound.Result.Builder> results, TypeBound.Result.Builder... constraints) {
        for (TypeBound.Result.Builder boundBuilder : constraints) {
            TypeBound bound = boundBuilder.bound();
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
        }

        Collections.addAll(results, constraints);
    }
}
