package honeyroasted.jype;

public interface Type {

    TypeString toSignature(TypeString.Context context);

    TypeString toDescriptor(TypeString.Context context);

    TypeString toSource(TypeString.Context context);

    TypeString toString(TypeString.Context context);

    default void lock() {}

    default boolean isArray() {
        return false;
    }

    default boolean isPrimitive() {
        return false;
    }

}
