package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionFailedException;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JMethodReference;

public class JReflectionMethodReferenceResolver implements JTypeResolver<JMethodLocation, JMethodReference> {

    @Override
    public JResolutionResult<JMethodLocation, JMethodReference> resolve(JTypeSystem system, JMethodLocation value) {
        JResolutionResult<JMethodLocation, JMethodReference> cached = system.storage().cacheFor(JMethodLocation.class).asResolution(value);
        if (cached.success()) {
            return cached;
        }

        try {
            return JResolutionResult.inherit(value, JReflectionTypeResolution.createMethodReference(system, JReflectionTypeResolution.methodFromLocation(value), value));
        } catch (JResolutionFailedException | JReflectionLookupException e) {
            return new JResolutionResult<>("Failed to lookup method from location", value, e);
        }
    }
}
