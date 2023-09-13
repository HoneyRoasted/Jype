package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.system.resolver.exception.ResolutionFailedException;
import honeyroasted.jype.type.Type;
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
            Optional<? extends Type> var = system.resolve(java.lang.reflect.Type.class, Type.class, ReflectionTypeResolution.typeParameterFromLocation(value));
            if (var.isPresent() && var.get() instanceof VarType vType) {
                return Optional.of(vType);
            } else {
                return Optional.empty();
            }
        } catch (ResolutionFailedException e) {
            return Optional.empty();
        }
    }

}
