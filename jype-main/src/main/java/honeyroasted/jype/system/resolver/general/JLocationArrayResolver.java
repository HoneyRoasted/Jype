package honeyroasted.jype.system.resolver.general;

import honeyroasted.jype.metadata.location.JClassLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JType;

public class JLocationArrayResolver implements JTypeResolver<JClassLocation, JType> {

    @Override
    public JResolutionResult<JClassLocation, JType> resolve(JTypeSystem system, JClassLocation value) {
        if (value.isArray()) {
            int depth = 0;
            JClassLocation component = value;

            while (component.isArray()) {
                depth++;
                component = component.containing();
            }

            JType result = system.tryResolve(component);

            for (int i = 0; i < depth; i++) {
                JArrayType containing = system.typeFactory().newArrayType();
                containing.setComponent(result);
                containing.setUnmodifiable(true);
                result = containing;
            }

            return new JResolutionResult<>(result, value);
        } else {
            return new JResolutionResult<>("Not an array type", value);
        }
    }

}
