package honeyroasted.jype.system.resolver;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class JBundledTypeResolvers implements JTypeResolver<Object, JType> {
    private List<JTypeResolver<?, ?>> resolvers;

    public JBundledTypeResolvers(List<JTypeResolver<?, ?>> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    public JBundledTypeResolvers(JTypeResolver<?, ?>... resolvers) {
        this.resolvers = List.of(resolvers);
    }

    @Override
    public Optional<? extends JType> resolve(JTypeSystem system, Object value) {
        for (JTypeResolver resolver : this.resolvers) {
            if (resolver.keyType().isInstance(value)) {
                Optional<? extends JType> attempt = resolver.resolve(system, value);
                if (attempt.isPresent()) {
                    return attempt;
                }
            }
        }
        return Optional.empty();
    }

    public List<JTypeResolver<?, ?>> resolvers() {
        List<JTypeResolver<?, ?>> flatResolvers = new ArrayList<>();
        for (JTypeResolver resolver : this.resolvers) {
            if (resolver instanceof JBundledTypeResolvers bundle) {
                flatResolvers.addAll(bundle.resolvers());
            } else {
                flatResolvers.add(resolver);
            }
        }
        return Collections.unmodifiableList(flatResolvers);
    }
}
