package honeyroasted.jype;

import honeyroasted.jype.type.TypeParameter;

import java.util.function.Consumer;
import java.util.function.Function;

public interface Type {

    default boolean isArray() {
        return false;
    }

    default boolean isPrimitive() {
        return false;
    }

    /**
     * Locks this {@link Type}, making it immutable. {@link Type}s may need to be constructed in a way that
     * requires mutability, but after that construction they can be locked.
     */
    default void lock() {

    }

}
