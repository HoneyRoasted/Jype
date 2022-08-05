package honeyroasted.jype.system.resolution;

import honeyroasted.jype.Type;
import honeyroasted.jype.type.TypeDeclaration;

import java.util.ArrayList;
import java.util.List;

public class SequentialTypeResolutionStrategy implements TypeResolutionStrategy {
    private List<TypeResolver<?, ?>> typeResolvers = new ArrayList<>();

    public SequentialTypeResolutionStrategy add(TypeResolver<?, ?> resolver) {
        this.typeResolvers.add(resolver);
        return this;
    }

    @Override
    public Type resolve(Object type) {
        for (TypeResolver resolver : this.typeResolvers) {
            if (resolver.typeClass().isInstance(type)) {
                return resolver.resolve(type);
            }
        }

        throw new IllegalArgumentException("Unrecognized type: " + (type == null ? "null" : type.getClass().getName()));
    }

    @Override
    public TypeDeclaration resolveDeclaration(Object type) {
        for (TypeResolver resolver : this.typeResolvers) {
            if (resolver.declarationClass().isInstance(type)) {
                return resolver.resolveDeclaration(type);
            }
        }
        throw new IllegalArgumentException("Unrecognized type: " + (type == null ? "null" : type.getClass().getName()));
    }
}
