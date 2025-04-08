package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.metadata.location.JMethodLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JMethodReference;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;

public class JReflectionJavaMethodResolver implements JTypeResolver<Executable, JMethodReference> {

    @Override
    public JResolutionResult<Executable, JMethodReference> resolve(JTypeSystem system, Executable value) {
        JResolutionResult<Executable, JMethodReference> cached = system.storage().cacheFor(Executable.class).asResolution(value);
        if (cached.success()) {
            return cached;
        }

        JMethodLocation location;

        if (value instanceof Method method) {
            location = JMethodLocation.of(method);
        } else if (value instanceof Constructor<?> constructor) {
            location = JMethodLocation.of(constructor);
        } else {
            return new JResolutionResult<>("Unknown executable type", value);
        }

        JResolutionResult<JMethodLocation, JMethodReference> locCached = system.storage().cacheFor(JMethodLocation.class).asResolution(location);
        if (locCached.success()) {
            return JResolutionResult.inherit(value, locCached);
        }

        JResolutionResult<JMethodLocation, JMethodReference> attemptByLocation = system.resolve(location);
        if (attemptByLocation.success()) {
            return JResolutionResult.inherit(value, attemptByLocation);
        } else {
            return JReflectionTypeResolution.createMethodReference(system, value, location);
        }
    }

}
