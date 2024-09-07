package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JType;

import java.util.Optional;

public class JReflectionTypeTokenResolver implements JTypeResolver<JTypeToken, JType> {

    @Override
    public Optional<? extends JType> resolve(JTypeSystem system, JTypeToken value) {
        return system.resolve(java.lang.reflect.Type.class, JType.class, value.extractType());
    }

}
