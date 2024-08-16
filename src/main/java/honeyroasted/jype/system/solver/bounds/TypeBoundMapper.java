package honeyroasted.jype.system.solver.bounds;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.Type;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface TypeBoundMapper {

    static TypeBoundMapper of(int arity, boolean commutative, Set<TypeBound.Classification> classification, Set<Class<? extends  TypeBound>> accepted, BiConsumer<Context, TypeBound.Result.Builder[]> consumer) {
        return new SimpleTypeBoundMapper(arity, commutative, classification, accepted, consumer);
    }

    static TypeBoundMapper simpleMapper(BiFunction<TypeSystem, TypeBound.Result.Builder, TypeBound.Result.Builder> operation) {
        return of(1, true, Set.of(TypeBound.Classification.BOUND, TypeBound.Classification.CONSTRAINT), Collections.emptySet(), (context, arr) -> {
            context.defaultConsumer().accept(operation.apply(context.system, arr[0]));
        });
    }

    default int arity() {
        return 1;
    }

    default boolean commutative() {
        return true;
    }


    default boolean accepts(TypeBound.Classification classification) {
        return classification == TypeBound.Classification.CONSTRAINT;
    }

    boolean accepts(TypeBound.Result.Builder builder);

    default boolean accepts(TypeBound.Result.Builder... input) {
        return true;
    }

    void map(Context context, TypeBound.Result.Builder... input);

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
                   TypeSystem system, TypeBound.Classification classification,
                   List<Function<Type, Type>> typeModifiers) {

        public Consumer<TypeBound.Result.Builder> consumerFor(TypeBound.Classification classification) {
            return classification == TypeBound.Classification.BOUND ? this.bounds : this.constraints;
        }

        public Consumer<TypeBound.Result.Builder> defaultConsumer() {
            return consumerFor(this.classification);
        }

        public Context addTypeModifier(Function<Type, Type> fn) {
            this.typeModifiers.add(fn);
            return this;
        }

        public <T extends Type> T view(Type type) {
            Type current = type;
            for (Function<Type, Type> mods : this.typeModifiers) {
                current = mods.apply(current);
            }
            return (T) current;
        }
    }
}
