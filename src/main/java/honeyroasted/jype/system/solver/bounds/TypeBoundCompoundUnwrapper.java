package honeyroasted.jype.system.solver.bounds;

public class TypeBoundCompoundUnwrapper implements UnaryTypeBoundMapper<TypeBound.Compound> {
    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.bound() instanceof TypeBound.Compound;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder constraint, TypeBound.Compound bound) {
        constraint.setPropagation(TypeBound.Result.Propagation.AND);
        bound.children().forEach(t -> context.defaultConsumer().accept(TypeBound.Result.builder(t, constraint)));
    }
}
