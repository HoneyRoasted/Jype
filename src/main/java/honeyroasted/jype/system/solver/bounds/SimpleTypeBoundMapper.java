package honeyroasted.jype.system.solver.bounds;

import java.util.Set;
import java.util.function.BiConsumer;

public class SimpleTypeBoundMapper implements TypeBoundMapper {
    private int arity;
    private boolean commutative;

    private Set<TypeBound.Classification> acceptedClassifications;
    private Set<Class<? extends TypeBound>> acceptedBounds;

    private BiConsumer<Context, TypeBound.Result.Builder[]> consumer;

    public SimpleTypeBoundMapper(int arity, boolean commutative, Set<TypeBound.Classification> acceptedClassifications, Set<Class<? extends TypeBound>> acceptedBounds, BiConsumer<Context, TypeBound.Result.Builder[]> consumer) {
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
    public boolean accepts(TypeBound.Classification classification) {
        return this.acceptedClassifications.contains(classification);
    }

    @Override
    public boolean accepts(TypeBound.Result.Builder builder) {
        return builder.bound() != null && (this.acceptedBounds.isEmpty() || this.acceptedBounds.stream().anyMatch(c -> c.isAssignableFrom(builder.bound().getClass())));
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
