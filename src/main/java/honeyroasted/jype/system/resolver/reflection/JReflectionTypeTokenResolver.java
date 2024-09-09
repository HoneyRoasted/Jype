package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JType;

import java.lang.reflect.Type;

public class JReflectionTypeTokenResolver implements JTypeResolver<JTypeToken, JType> {

    @Override
    public JResolutionResult<JTypeToken, JType> resolve(JTypeSystem system, JTypeToken value) {
        return JResolutionResult.inherit(value, system.resolve(Type.class, JType.class, value.extractType()));
    }

}
