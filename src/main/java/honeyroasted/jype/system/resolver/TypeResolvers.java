package honeyroasted.jype.system.resolver;

import honeyroasted.jype.type.Type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TypeResolvers {
    private Map<Class, List<TypeResolver<?, ?>>> resolvers = new LinkedHashMap<>();

    public <I, O extends Type> void register(TypeResolver<I, O> resolver) {
        this.resolvers.computeIfAbsent(resolver.keyType(), c -> new ArrayList<>()).add(resolver);
    }

    public <I, O extends Type> void clear(Class<I> keyType) {
        this.resolvers.remove(keyType);
    }

    public <I, O extends Type> TypeResolver<I, O> resolverFor(Class<I> keyType, Class<O> outputType) {
        List<TypeResolver<?, ?>> applicable = this.resolvers.get(keyType);

        if (applicable == null || applicable.isEmpty()) {
            return TypeResolver.none();
        } else {
            return new MultiTypeResolver<>((List) applicable.stream().filter(t -> outputType.isAssignableFrom(t.resultType())).toList());
        }
    }

}
