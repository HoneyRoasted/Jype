package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.metadata.location.JFieldLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionFailedException;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JFieldReference;

public class JReflectionFieldReferenceResolver implements JTypeResolver<JFieldLocation, JFieldReference> {

    @Override
    public JResolutionResult<JFieldLocation, JFieldReference> resolve(JTypeSystem system, JFieldLocation value) {
        JResolutionResult<JFieldLocation, JFieldReference> cached = system.storage().cacheFor(JFieldLocation.class).asResolution(value);
        if (cached.success()) {
            return cached;
        }

        try {
            return JResolutionResult.inherit(value, JReflectionTypeResolution.createFieldReference(system, JReflectionTypeResolution.fieldFromLocation(value), value));
        } catch (JResolutionFailedException | JReflectionLookupException e) {
            return new JResolutionResult<>("Failed to lookup field from location via reflection", value, e);
        }
    }
}
