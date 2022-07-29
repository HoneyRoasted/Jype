package honeyroasted.jype;

public interface Type {

    TypeString toSignature(TypeString.Context context);

    TypeString toDescriptor(TypeString.Context context);

    TypeString toSource(TypeString.Context context);

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
