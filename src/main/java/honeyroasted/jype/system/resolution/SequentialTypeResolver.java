package honeyroasted.jype.system.resolution;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.type.TypeDeclaration;

import java.util.List;

public class SequentialTypeResolver implements TypeResolver<Object, Object> {
    private List<TypeResolver> resolvers;

    public SequentialTypeResolver(List<TypeResolver<?, ?>> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    @Override
    public TypeConcrete resolve(Object type) {
        for (TypeResolver resolver : this.resolvers) {
            if (resolver.acceptsType(type)) {
                TypeConcrete concrete = resolver.resolve(type);
                if (concrete != null) {
                    return concrete;
                }
            }
        }
        return null;
    }

    @Override
    public TypeDeclaration resolveDeclaration(Object type) {
        for (TypeResolver resolver : this.resolvers) {
            if (resolver.acceptsDeclaration(type)) {
                TypeDeclaration declaration = resolver.resolveDeclaration(type);
                if (declaration != null) {
                    return declaration;
                }
            }
        }
        return null;
    }

    @Override
    public boolean acceptsType(Object type) {
        return this.resolvers.stream().anyMatch(t -> t.acceptsType(type));
    }

    @Override
    public boolean acceptsDeclaration(Object type) {
        return this.resolvers.stream().anyMatch(t -> t.acceptsDeclaration(type));
    }
}
