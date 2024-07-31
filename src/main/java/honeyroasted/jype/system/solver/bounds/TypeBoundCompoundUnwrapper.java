package honeyroasted.jype.system.solver.bounds;

import java.util.List;

public class TypeBoundCompoundUnwrapper implements UnaryTypeBoundMapper<TypeBound.Compound> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.bound() instanceof TypeBound.Compound;
    }

    @Override
    public void map(List<TypeBound.Result.Builder> bounds, List<TypeBound.Result.Builder> constraints, TypeBound.Classification classification, TypeBound.Result.Builder constraint, TypeBound.Compound bound) {
        constraint.setPropagation(TypeBound.Result.Propagation.AND);
        switch (classification) {
            case CONSTRAINT -> bound.children().forEach(t -> constraints.add(TypeBound.Result.builder(t, constraint)));
            case BOUND, BOTH -> bound.children().forEach(t -> bounds.add(TypeBound.Result.builder(t, constraint)));
        }
    }
}
