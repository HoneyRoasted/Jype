package honeyroasted.jype.system.solver.bounds;

import java.util.Set;

public class TypeBoundCompoundUnwrapper implements UnaryTypeBoundMapper<TypeBound.Compound> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.bound() instanceof TypeBound.Compound;
    }

    @Override
    public void map(Set<TypeBound.Result.Builder> results, TypeBound.Result.Builder constraint, TypeBound.Compound bound) {
        constraint.setPropagation(TypeBound.Result.Propagation.AND);
        bound.children().forEach(t -> results.add(TypeBound.Result.builder(t, constraint)));
    }
}
