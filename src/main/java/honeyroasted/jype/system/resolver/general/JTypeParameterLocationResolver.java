package honeyroasted.jype.system.resolver.general;

import honeyroasted.jype.location.JClassNamespace;
import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.location.JTypeParameterLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JVarType;

import java.util.Optional;

public class JTypeParameterLocationResolver implements JTypeResolver<JTypeParameterLocation, JVarType> {

    @Override
    public Optional<? extends JVarType> resolve(JTypeSystem system, JTypeParameterLocation value) {
        if (value.containing() instanceof JMethodLocation mloc) {
            Optional<JMethodReference> method = system.resolve(mloc);
            if (method.isPresent()) {
                return method.get().typeParameters().stream().filter(j -> j.location().equals(value)).findFirst();
            }
        } else if (value.containing() instanceof JClassNamespace cloc) {
            Optional<JClassReference> cls = system.resolve(cloc.location());
            if (cls.isPresent()) {
                return cls.get().typeParameters().stream().filter(j -> j.location().equals(value)).findFirst();
            }
        }
        return Optional.empty();
    }

}
