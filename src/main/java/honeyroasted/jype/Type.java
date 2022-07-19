package honeyroasted.jype;

public interface Type {

    default boolean isArray() {
        return false;
    }

    default boolean isPrimitive() {
        return false;
    }

}
