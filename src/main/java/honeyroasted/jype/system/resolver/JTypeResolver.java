package honeyroasted.jype.system.resolver;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JType;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

public interface JTypeResolver<I, O extends JType> {

    static <I, O extends JType> JTypeResolver<I, O> none() {
        return (s, v) -> Optional.empty();
    }

    Optional<? extends O> resolve(JTypeSystem system, I value);

    default Class<I> keyType() {
        return (Class<I>) ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
    }

    default Class<O> resultType() {
        return (Class<O>) ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[1];
    }

}