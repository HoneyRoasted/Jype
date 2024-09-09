package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionFailedException;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JType;

public class JReflectionClassReferenceResolver implements JTypeResolver<JClassLocation, JType> {
    @Override
    public JResolutionResult<JClassLocation, JType> resolve(JTypeSystem system, JClassLocation value) {
        JResolutionResult<JClassLocation, JType> cached = system.storage().cacheFor(JClassLocation.class).asResolution(value);
        if (cached.success()) {
            return cached;
        }

        try {
            return JResolutionResult.inherit(value, JReflectionTypeResolution.createClassReference(system, JReflectionTypeResolution.classFromLocation(value), value));
        } catch (JResolutionFailedException | JReflectionLookupException e) {
            return new JResolutionResult<>("Failed to lookup class from location via reflection", value, e);
        }
    }

}
