package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.system.resolver.ResolutionFailedException;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.Type;

import java.util.Optional;

public class ReflectionMethodReferenceResolver implements TypeResolver<MethodLocation, MethodReference> {

    @Override
    public Optional<? extends MethodReference> resolve(TypeSystem system, MethodLocation value) {
        Optional<Type> cached = system.storage().cacheFor(MethodLocation.class).get(value);
        if (cached.isPresent() && cached.get() instanceof MethodReference ref) {
            return Optional.of(ref);
        }

        try {
            return ReflectionTypeResolution.createMethodReference(system, ReflectionTypeResolution.methodFromLocation(value), value);
        } catch (ResolutionFailedException e) {
            return Optional.empty();
        }
    }
}
