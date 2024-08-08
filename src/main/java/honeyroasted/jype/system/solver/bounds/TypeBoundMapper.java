package honeyroasted.jype.system.solver.bounds;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public interface TypeBoundMapper {

    static TypeBoundMapper of(int arity, boolean commutative, TypeBound.Classification classification, Set<Class<? extends  TypeBound>> accepted, BiConsumer<Context, TypeBound.Result.Builder[]> consumer) {
        return new SimpleTypeBoundMapper(arity, commutative, classification, accepted, consumer);
    }

    static TypeBoundMapper simpleMapper(BiFunction<TypeSystem, TypeBound.Result.Builder, TypeBound.Result.Builder> operation) {
        return of(1, true, TypeBound.Classification.BOTH, Collections.emptySet(), (context, arr) -> {
            context.defaultConsumer().accept(operation.apply(context.system, arr[0]));
        });
    }

    default int arity() {
        return 1;
    }

    default boolean commutative() {
        return true;
    }


    default TypeBound.Classification classification() {
        return TypeBound.Classification.BOUND;
    }

    boolean accepts(TypeBound.Result.Builder builder);

    default boolean accepts(TypeBound.Result.Builder... input) {
        return true;
    }

    void map(Context context, TypeBound.Result.Builder... input);

    default Pair<List<TypeBound.Result.Builder>, List<TypeBound.Result.Builder>> map(TypeSystem system, TypeBound.Classification classification, TypeBound.Result.Builder... input) {
        List<TypeBound.Result.Builder> bounds = new ArrayList<>();
        List<TypeBound.Result.Builder> constraints = new ArrayList<>();
        this.map(new Context(bounds::add, constraints::add, bounds, constraints, system, classification), input);
        return Pair.of(bounds, constraints);
    }

    default Pair<List<TypeBound.Result.Builder>, List<TypeBound.Result.Builder>> map(TypeSystem system, TypeBound.Classification classification, List<TypeBound.Result.Builder> input) {
        return this.map(system, classification, input.toArray(TypeBound.Result.Builder[]::new));
    }

    default <T> void addAll(Consumer<? super T> collection, T... arr) {
        for (T t : arr) {
            collection.accept(t);
        }
    }

    default <T> void addAll(Consumer<? super T> collection, Iterable<T> toAdd) {
        for (T t : toAdd) {
            collection.accept(t);
        }
    }

    record Context(Consumer<TypeBound.Result.Builder> bounds, Consumer<TypeBound.Result.Builder> constraints,
                   Collection<TypeBound.Result.Builder> currentBounds, Collection<TypeBound.Result.Builder> currentConstraints,
                   TypeSystem system, TypeBound.Classification classification) {

        public Consumer<TypeBound.Result.Builder> consumerFor(TypeBound.Classification classification) {
            return classification == TypeBound.Classification.BOUND ? this.bounds : this.constraints;
        }

        public Consumer<TypeBound.Result.Builder> defaultConsumer() {
            return consumerFor(this.classification);
        }

    }
}
