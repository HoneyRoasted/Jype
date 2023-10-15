package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.ResolutionFailedException;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.Type;

import java.util.Optional;

public class ReflectionClassReferenceResolver implements TypeResolver<ClassLocation, Type> {
    @Override
    public Optional<Type> resolve(TypeSystem system, ClassLocation value) {
        Optional<Type> cached = system.storage().cacheFor(ClassLocation.class).get(value);
        if (cached.isPresent() && cached.get() instanceof ClassReference cRef) {
            return Optional.of(cRef);
        }

        try {
            return ReflectionTypeResolution.createClassReference(system, ReflectionTypeResolution.classFromLocation(value), value);
        } catch (ResolutionFailedException e) {
            return Optional.empty();
        }
    }


}
