package honeyroasted.jype.system.resolver;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.Type;

import java.util.List;
import java.util.Optional;

public class MultiTypeResolver<I, O extends Type> implements TypeResolver<I, O> {
    private List<TypeResolver<I, ? extends O>> resolvers;

    public MultiTypeResolver(List<TypeResolver<I, ? extends O>> resolvers) {
        this.resolvers = resolvers;
    }

    @Override
    public Optional<? extends O> resolve(TypeSystem system, I value) {
        for (TypeResolver<I, ? extends O> resolver : this.resolvers) {
            Optional<? extends O> attempt = resolver.resolve(system, value);
            if (attempt.isPresent()) {
                return attempt;
            }
        }

        return Optional.empty();
    }


}
