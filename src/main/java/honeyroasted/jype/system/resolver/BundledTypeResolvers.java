package honeyroasted.jype.system.resolver;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BundledTypeResolvers implements TypeResolver<Object, Type> {
    private List<TypeResolver<?, ?>> resolvers;

    public BundledTypeResolvers(List<TypeResolver<?, ?>> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    public BundledTypeResolvers(TypeResolver<?, ?>... resolvers) {
        this.resolvers = List.of(resolvers);
    }

    @Override
    public Optional<? extends Type> resolve(TypeSystem system, Object value) {
        for (TypeResolver resolver : this.resolvers) {
            if (resolver.keyType().isInstance(value)) {
                Optional<? extends Type> attempt = resolver.resolve(system, value);
                if (attempt.isPresent()) {
                    return attempt;
                }
            }
        }
        return Optional.empty();
    }

    public List<TypeResolver<?, ?>> resolvers() {
        List<TypeResolver<?, ?>> flatResolvers = new ArrayList<>();
        for (TypeResolver resolver : this.resolvers) {
            if (resolver instanceof BundledTypeResolvers bundle) {
                flatResolvers.addAll(bundle.resolvers());
            } else {
                flatResolvers.add(resolver);
            }
        }
        return Collections.unmodifiableList(flatResolvers);
    }
}
