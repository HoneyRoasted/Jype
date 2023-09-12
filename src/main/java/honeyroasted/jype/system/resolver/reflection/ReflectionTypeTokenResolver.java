package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.type.Type;

import java.util.Optional;

public class ReflectionTypeTokenResolver implements TypeResolver<TypeToken, Type> {

    @Override
    public Optional<? extends Type> resolve(TypeSystem system, TypeToken value) {
        return system.resolve(java.lang.reflect.Type.class, Type.class, value.extractType());
    }

}
