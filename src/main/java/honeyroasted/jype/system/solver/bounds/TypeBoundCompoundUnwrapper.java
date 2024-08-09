package honeyroasted.jype.system.solver.bounds;

public class TypeBoundCompoundUnwrapper implements UnaryTypeBoundMapper<TypeBound.Compound> {

    @Override
    public TypeBound.Classification classification() {
        return TypeBound.Classification.BOTH;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder builder, TypeBound.Compound bound) {
        builder.setPropagation(TypeBound.Result.Propagation.AND);
        bound.children().forEach(t -> context.defaultConsumer().accept(TypeBound.Result.builder(t, builder)));
    }
}
