package honeyroasted.jype.system.resolver;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JType;

import java.util.List;
import java.util.Optional;

public class JMultiTypeResolvers<I, O extends JType> implements JTypeResolver<I, O> {
    private List<JTypeResolver<I, ? extends O>> resolvers;

    public JMultiTypeResolvers(List<JTypeResolver<I, ? extends O>> resolvers) {
        this.resolvers = resolvers;
    }

    public JMultiTypeResolvers(JTypeResolver<I, ? extends O>... resolvers) {
        this(List.of(resolvers));
    }

    @Override
    public Optional<? extends O> resolve(JTypeSystem system, I value) {
        for (JTypeResolver<I, ? extends O> resolver : this.resolvers) {
            Optional<? extends O> attempt = resolver.resolve(system, value);
            if (attempt.isPresent()) {
                return attempt;
            }
        }

        return Optional.empty();
    }


}
