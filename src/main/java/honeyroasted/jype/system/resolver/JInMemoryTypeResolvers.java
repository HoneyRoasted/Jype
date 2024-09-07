package honeyroasted.jype.system.resolver;

import honeyroasted.jype.type.JType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JInMemoryTypeResolvers implements JTypeResolvers {
    private List<JTypeResolver<?, ?>> resolvers = new ArrayList<>();

    @Override
    public <I, O extends JType> void register(JTypeResolver<I, O> resolver) {
        this.resolvers.add(resolver);
    }

    @Override
    public <I> void clear(Class<I> keyType) {
        this.resolvers.removeIf(t -> t.keyType().isAssignableFrom(keyType));
    }

    @Override
    public <I, O extends JType> JTypeResolver<I, O> resolverFor(Class<I> keyType, Class<O> outputType) {
        return (system, value) -> {
            for (JTypeResolver<?, ?> resolver : this.resolvers) {
                if (resolver.keyType().isAssignableFrom(keyType) && outputType.isAssignableFrom(resolver.resultType())) {
                    Optional<? extends O> attempt = ((JTypeResolver<? super I, ? extends O>) resolver).resolve(system, value);
                    if (attempt.isPresent()) {
                        return attempt;
                    }
                }
            }
            return Optional.empty();
        };
    }


}
