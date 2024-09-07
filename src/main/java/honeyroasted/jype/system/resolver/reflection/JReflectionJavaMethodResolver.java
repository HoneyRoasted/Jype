package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Optional;

public class JReflectionJavaMethodResolver implements JTypeResolver<Executable, JMethodReference> {

    @Override
    public Optional<? extends JMethodReference> resolve(JTypeSystem system, Executable value) {
        Optional<JType> cached = system.storage().cacheFor(Executable.class).get(value);
        if (cached.isPresent() && cached.get() instanceof JMethodReference mRef) {
            return Optional.of(mRef);
        }

        JMethodLocation location;

        if (value instanceof Method method) {
            location = JMethodLocation.of(method);
        } else if (value instanceof Constructor<?> constructor) {
            location = JMethodLocation.of(constructor);
        } else {
            return Optional.empty();
        }

        Optional<JType> locCached = system.storage().cacheFor(JMethodLocation.class).get(location);
        if (locCached.isPresent() && locCached.get() instanceof JMethodReference mRef) {
            return Optional.of(mRef);
        }

        Optional<? extends JMethodReference> attemptByLocation = system.resolve(location);
        if (attemptByLocation.isPresent()) {
            return attemptByLocation;
        } else {
            return JReflectionTypeResolution.createMethodReference(system, value, location);
        }
    }

}
