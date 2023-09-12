package honeyroasted.jype.system.resolver;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.Type;

import java.util.Optional;

public class ReflectionTypeResolver implements TypeResolver<java.lang.reflect.Type, Type> {

    @Override
    public Optional<Type> resolve(TypeSystem system, TypeCache<java.lang.reflect.Type, Type> cache, java.lang.reflect.Type value) {
        if (cache.contains(value)) {
            return cache.get(value);
        }

        return Optional.empty();
    }

    @Override
    public Class<java.lang.reflect.Type> inputType() {
        return java.lang.reflect.Type.class;
    }

    @Override
    public Class<Type> outputType() {
        return Type.class;
    }

}
