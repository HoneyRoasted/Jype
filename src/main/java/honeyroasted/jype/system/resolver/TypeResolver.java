package honeyroasted.jype.system.resolver;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.Type;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

public interface TypeResolver<I, O extends Type> {

    static <I, O extends Type> TypeResolver<I, O> none() {
        return (s, v) -> Optional.empty();
    }

    Optional<? extends O> resolve(TypeSystem system, I value);

    default Class<I> keyType() {
        return (Class<I>) ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
    }

    default Class<O> resultType() {
        return (Class<O>) ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[1];
    }

}
