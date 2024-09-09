package honeyroasted.jype.system.resolver;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JBundledTypeResolvers implements JTypeResolver<Object, JType> {
    private List<JTypeResolver<?, ?>> resolvers;

    public JBundledTypeResolvers(List<JTypeResolver<?, ?>> resolvers) {
        this.resolvers = resolvers;
    }

    public JBundledTypeResolvers(JTypeResolver<?, ?>... resolvers) {
        this.resolvers = List.of(resolvers);
    }

    @Override
    public JResolutionResult<Object, JType> resolve(JTypeSystem system, Object value) {
        List<JResolutionResult<?, JType>> building = new ArrayList<>();

        for (JTypeResolver resolver : this.resolvers) {
            if (resolver.keyType().isInstance(value)) {
                JResolutionResult<?, JType> attempt = resolver.resolve(system, value);
                building.add(attempt);
                if (attempt.success()) {
                    break;
                }
            }
        }

        return JResolutionResult.inherit(value, building);
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
