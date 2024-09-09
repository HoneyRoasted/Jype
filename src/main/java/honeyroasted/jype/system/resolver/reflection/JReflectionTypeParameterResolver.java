package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.JTypeParameterLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionFailedException;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JVarType;

public class JReflectionTypeParameterResolver implements JTypeResolver<JTypeParameterLocation, JVarType> {
    @Override
    public JResolutionResult<JTypeParameterLocation, JVarType> resolve(JTypeSystem system, JTypeParameterLocation value) {
        JResolutionResult<JTypeParameterLocation, JVarType> cached = system.storage().<JTypeParameterLocation, JVarType>cacheFor(JTypeParameterLocation.class).asResolution(value);
        if (cached.success()) {
            return cached;
        }

        try {
            return JResolutionResult.inherit(value, JReflectionTypeResolution.createVarType(system, JReflectionTypeResolution.typeParameterFromLocation(value), value));
        } catch (JResolutionFailedException | JReflectionLookupException e) {
            return new JResolutionResult<>("Failed to lookup type parameter from location via reflection", value, e);
        }
    }

}
