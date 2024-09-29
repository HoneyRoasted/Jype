package honeyroasted.jype.system.resolver.general;

import honeyroasted.jype.location.JClassNamespace;
import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.location.JTypeParameterLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JVarType;

public class JTypeParameterLocationResolver implements JTypeResolver<JTypeParameterLocation, JVarType> {

    @Override
    public JResolutionResult<JTypeParameterLocation, JVarType> resolve(JTypeSystem system, JTypeParameterLocation value) {
        JResolutionResult<JTypeParameterLocation, JVarType> cached = system.storage().cacheFor(JTypeParameterLocation.class).asResolution(value);
        if (cached.success()) {
            return cached;
        }

        if (value.containing() instanceof JMethodLocation mloc) {
            return system.resolve(mloc).flatMap(value,
                    ref -> ref.typeParameters().stream().filter(j -> j.location().equals(value)).findFirst(),
                    "Could not find " + value.name() + " in " + value.containing());
        } else if (value.containing() instanceof JClassNamespace cloc) {
            return system.<JClassReference>resolve(cloc.location()).flatMap(value,
                    ref -> ref.typeParameters().stream().filter(j -> j.location().equals(value)).findFirst(),
                    "Could not find " + value.name() + " in " + value.containing());
        }

        return new JResolutionResult<>("Unknown containing type: " + value.containing().getClass().getName(), value);
    }

}
