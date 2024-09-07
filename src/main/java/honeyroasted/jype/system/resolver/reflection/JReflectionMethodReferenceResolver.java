package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionFailedException;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;

import java.util.Optional;

public class JReflectionMethodReferenceResolver implements JTypeResolver<JMethodLocation, JMethodReference> {

    @Override
    public Optional<? extends JMethodReference> resolve(JTypeSystem system, JMethodLocation value) {
        Optional<JType> cached = system.storage().cacheFor(JMethodLocation.class).get(value);
        if (cached.isPresent() && cached.get() instanceof JMethodReference ref) {
            return Optional.of(ref);
        }

        try {
            return JReflectionTypeResolution.createMethodReference(system, JReflectionTypeResolution.methodFromLocation(value), value);
        } catch (JResolutionFailedException e) {
            return Optional.empty();
        }
    }
}
