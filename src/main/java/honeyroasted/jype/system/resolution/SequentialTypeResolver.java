package honeyroasted.jype.system.resolution;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.type.TypeDeclaration;

import java.util.List;

/**
 * This is a simple {@link TypeResolver} that attempts to resolve types with a list of other {@link TypeResolver}s.
 * It will return the first successful, non-null resolution from one of those children resolvers.
 */
public class SequentialTypeResolver implements TypeResolver<Object, Object> {
    private List<TypeResolver> resolvers;

    /**
     * Creates a new {@link SequentialTypeResolver}
     *
     * @param resolvers The children {@link TypeResolver}s to use (in order) for type resolution
     */
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
