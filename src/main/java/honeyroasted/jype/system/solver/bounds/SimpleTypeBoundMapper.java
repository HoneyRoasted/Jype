package honeyroasted.jype.system.solver.bounds;

import java.util.Set;
import java.util.function.BiConsumer;

public class SimpleTypeBoundMapper implements TypeBoundMapper {
    private int arity;
    private boolean commutative;

    private TypeBound.Classification acceptedClassifications;
    private Set<Class<? extends TypeBound>> acceptedBounds;

    private BiConsumer<Context, TypeBound.Result.Builder[]> consumer;

    public SimpleTypeBoundMapper(int arity, boolean commutative, TypeBound.Classification acceptedClassifications, Set<Class<? extends TypeBound>> acceptedBounds, BiConsumer<Context, TypeBound.Result.Builder[]> consumer) {
        this.arity = arity;
        this.commutative = commutative;
        this.acceptedClassifications = acceptedClassifications;
        this.acceptedBounds = acceptedBounds;
        this.consumer = consumer;
    }

    @Override
    public int arity() {
        return this.arity;
    }

    @Override
    public TypeBound.Classification classification() {
        return this.acceptedClassifications;
    }

    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return constraint.bound() != null && (this.acceptedBounds.isEmpty() || this.acceptedBounds.stream().anyMatch(c -> c.isAssignableFrom(constraint.bound().getClass())));
    }

    @Override
    public boolean commutative() {
        return this.commutative;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder... input) {
        this.consumer.accept(context, input);
    }
}
