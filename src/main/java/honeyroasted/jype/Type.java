package honeyroasted.jype;

import honeyroasted.jype.concrete.TypeParameterReference;
import honeyroasted.jype.declaration.TypeParameter;

import java.util.function.Consumer;
import java.util.function.Function;

public interface Type {

    default boolean isArray() {
        return false;
    }

    default boolean isPrimitive() {
        return false;
    }

    default <T extends Type> T map(Function<Type, Type> mapper) {
        return (T) mapper.apply(this);
    }

    default void forEach(Consumer<Type> consumer) {
        map(t -> {
            consumer.accept(t);
            return t;
        });
    }

    default <T extends Type> T copy() {
        return map(Function.identity());
    }

    default <T extends Type> T resolveVariables(Function<TypeParameter, Type> mapper) {
        return map(t -> {
            if (t instanceof TypeParameter p) {
                return mapper.apply(p);
            } else if (t instanceof TypeParameterReference ref) {
                return mapper.apply(ref.variable());
            } else {
                return t;
            }
        });
    }

    /**
     * Locks this {@link Type}, making it immutable. {@link Type}s may need to be constructed in a way that
     * requires mutability, but after that construction they can be locked.
     */
    default void lock() {

    }

}
