package honeyroasted.jype;

import honeyroasted.jype.type.TypeParameter;

import java.util.function.Consumer;
import java.util.function.Function;

public interface TypeConcrete extends Type {

    default <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(this);
    }

    default TypeConcrete flatten() {
        return this;
    }

    boolean equalsExactly(TypeConcrete other);

    int hashCodeExactly();

    default void forEach(Consumer<TypeConcrete> consumer) {
        map(t -> {
            consumer.accept(t);
            return t;
        });
    }

    default <T extends TypeConcrete> T copy() {
        return map(Function.identity());
    }

    default <T extends Type> T resolveVariables(Function<TypeParameter, TypeConcrete> mapper) {
        return map(t -> {
            if (t instanceof TypeParameter ref) {
                return mapper.apply(ref);
            } else {
                return t;
            }
        });
    }

}
