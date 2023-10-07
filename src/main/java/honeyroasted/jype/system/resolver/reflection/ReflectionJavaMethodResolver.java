package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Optional;

public class ReflectionJavaMethodResolver implements TypeResolver<Executable, MethodReference> {

    @Override
    public Optional<? extends MethodReference> resolve(TypeSystem system, Executable value) {
        Optional<Type> cached = system.storage().cacheFor(Executable.class).get(value);
        if (cached.isPresent() && cached.get() instanceof MethodReference mRef) {
            return Optional.of(mRef);
        }

        MethodLocation location;

        if (value instanceof Method method) {
            location = MethodLocation.of(method);
        } else if (value instanceof Constructor<?> constructor) {
            location = MethodLocation.of(constructor);
        } else {
            return Optional.empty();
        }

        Optional<Type> locCached = system.storage().cacheFor(MethodLocation.class).get(location);
        if (locCached.isPresent() && locCached.get() instanceof MethodReference mRef) {
            return Optional.of(mRef);
        }

        Optional<? extends MethodReference> attemptByLocation = system.resolve(location);
        if (attemptByLocation.isPresent()) {
            return attemptByLocation;
        } else {
            return ReflectionTypeResolution.createMethodReference(system, value, location);
        }
    }

}
