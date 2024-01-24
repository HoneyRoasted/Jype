package honeyroasted.jype.system.resolver;

import honeyroasted.jype.type.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TypeResolvers {
    private List<TypeResolver<?, ?>> resolvers = new ArrayList<>();

    public <I, O extends Type> void register(TypeResolver<I, O> resolver) {
        this.resolvers.add(resolver);
    }

    public <I> void clear(Class<I> keyType) {
        this.resolvers.removeIf(t -> t.keyType().isAssignableFrom(keyType));
    }

    public <I, O extends Type> TypeResolver<I, O> resolverFor(Class<I> keyType, Class<O> outputType) {
        return (system, value) -> {
            for (TypeResolver<?, ?> resolver : this.resolvers) {
                if (resolver.keyType().isAssignableFrom(keyType) && outputType.isAssignableFrom(resolver.resultType())) {
                    Optional<? extends O> attempt = ((TypeResolver<? super I, ? extends O>) resolver).resolve(system, value);
                    if (attempt.isPresent()) {
                        return attempt;
                    }
                }
            }
            return Optional.empty();
        };
    }


}
