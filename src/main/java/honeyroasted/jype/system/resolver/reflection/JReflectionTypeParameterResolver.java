package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.JTypeParameterLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionFailedException;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JVarType;

import java.util.Optional;

public class JReflectionTypeParameterResolver implements JTypeResolver<JTypeParameterLocation, JVarType> {
    @Override
    public Optional<JVarType> resolve(JTypeSystem system, JTypeParameterLocation value) {
        Optional<JVarType> cached = system.storage().<JTypeParameterLocation, JVarType>cacheFor(JTypeParameterLocation.class).get(value);
        if (cached.isPresent()) {
            return cached;
        }

        try {
            return JReflectionTypeResolution.createVarType(system, JReflectionTypeResolution.typeParameterFromLocation(value), value);
        } catch (JResolutionFailedException e) {
            return Optional.empty();
        }
    }

}
