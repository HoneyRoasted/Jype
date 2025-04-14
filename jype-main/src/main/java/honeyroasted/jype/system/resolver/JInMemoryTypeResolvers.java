package honeyroasted.jype.system.resolver;

import honeyroasted.jype.type.JType;

import java.util.ArrayList;
import java.util.List;

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
            List<JResolutionResult<I, O>> building = new ArrayList<>();

            for (JTypeResolver resolver : this.resolvers) {
                if (resolver.keyType().isAssignableFrom(keyType) && outputType.isAssignableFrom(resolver.resultType())) {
                    JResolutionResult<I, O> attempt = resolver.resolve(system, value);
                    building.add(attempt);
                    if (attempt.success()) {
                        break;
                    }
                }
            }

            return building.size() == 1 ? building.get(0) : JResolutionResult.inherit(value, building);
        };
    }


}
