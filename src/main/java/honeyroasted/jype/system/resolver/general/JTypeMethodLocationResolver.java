package honeyroasted.jype.system.resolver.general;

import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JMethodReference;

public class JTypeMethodLocationResolver implements JTypeResolver<JMethodLocation, JMethodReference> {
    @Override
    public JResolutionResult<JMethodLocation, JMethodReference> resolve(JTypeSystem system, JMethodLocation value) {
        JResolutionResult<JMethodLocation, JMethodReference> cached = system.storage().cacheFor(JMethodLocation.class).asResolution(value);
        if (cached.success()) {
            return cached;
        }

        return system.<JClassReference>resolve(value.containing()).flatMap(value,
                ref -> ref.declaredMethods().stream().filter(m -> m.location().equals(value)).findFirst(),
                "Could not find " + value + " in " + value.containing());
    }
}
