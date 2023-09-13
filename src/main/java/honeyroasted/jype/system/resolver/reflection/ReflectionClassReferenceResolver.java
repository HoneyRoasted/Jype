package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.system.resolver.exception.ResolutionFailedException;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.Type;

import java.util.Optional;

public class ReflectionClassReferenceResolver implements TypeResolver<ClassLocation, ClassReference> {

    @Override
    public Optional<ClassReference> resolve(TypeSystem system, ClassLocation value) {
        Optional<Type> cached = system.storage().cacheFor(ClassLocation.class).get(value);
        if (cached.isPresent() && cached.get() instanceof ClassReference cRef) {
            return Optional.of(cRef);
        }

        try {
            Optional<? extends Type> type = system.resolve(ReflectionTypeResolution.classFromLocation(value));
            if (type.isPresent() && type.get() instanceof ClassReference ref) {
                return Optional.of(ref);
            } else {
                return Optional.empty();
            }
        } catch (ResolutionFailedException e) {
            return Optional.empty();
        }
    }



}
