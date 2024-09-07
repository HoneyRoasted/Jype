package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionFailedException;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JType;

import java.util.Optional;

public class JReflectionClassReferenceResolver implements JTypeResolver<JClassLocation, JType> {
    @Override
    public Optional<JType> resolve(JTypeSystem system, JClassLocation value) {
        Optional<JType> cached = system.storage().cacheFor(JClassLocation.class).get(value);
        if (cached.isPresent() && cached.get() instanceof JClassReference cRef) {
            return Optional.of(cRef);
        }

        try {
            return JReflectionTypeResolution.createClassReference(system, JReflectionTypeResolution.classFromLocation(value), value);
        } catch (JResolutionFailedException e) {
            return Optional.empty();
        }
    }


}
