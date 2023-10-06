package honeyroasted.jype.system.resolver;

import honeyroasted.jype.type.Type;

import java.util.ArrayList;
import java.util.List;

public class TypeResolvers {
    private List<TypeResolver<?, ?>> resolvers = new ArrayList<>();

    public <I, O extends Type> void register(TypeResolver<I, O> resolver) {
        this.resolvers.add(resolver);
    }

    public <I> void clear(Class<I> keyType) {
        this.resolvers.removeIf(t -> t.keyType().isAssignableFrom(keyType));
    }

    public <I, O extends Type> TypeResolver<I, O> resolverFor(Class<I> keyType, Class<O> outputType) {
        List<TypeResolver<?, ?>> applicable = this.resolvers.stream()
                .filter(t -> t.keyType().isAssignableFrom(keyType) && outputType.isAssignableFrom(t.resultType())).toList();

        if (applicable == null || applicable.isEmpty()) {
            return TypeResolver.none();
        } else if (applicable.size() == 1) {
            return (TypeResolver<I, O>) applicable.get(0);
        } else {
            return new MultiTypeResolvers<I, O>((List) applicable);
        }
    }

}
