package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.system.resolver.ResolutionFailedException;
import honeyroasted.jype.type.VarType;

import java.util.Optional;

public class ReflectionTypeParameterResolver implements TypeResolver<TypeParameterLocation, VarType> {
    @Override
    public Optional<VarType> resolve(TypeSystem system, TypeParameterLocation value) {
        Optional<VarType> cached = system.storage().<TypeParameterLocation, VarType>cacheFor(TypeParameterLocation.class).get(value);
        if (cached.isPresent()) {
            return cached;
        }

        try {
            return ReflectionTypeResolution.createVarType(system, ReflectionTypeResolution.typeParameterFromLocation(value), value);
        } catch (ResolutionFailedException e) {
            return Optional.empty();
        }
    }

}
