package honeyroasted.jype.system.resolver.general;

import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JType;

public class JLocationPrimitiveResolver implements JTypeResolver<JClassLocation, JType> {

    @Override
    public JResolutionResult<JClassLocation, JType> resolve(JTypeSystem system, JClassLocation value) {
        if (value.getPackage().isDefaultPackage()) {
            if ("void".equals(value.value())) {
                return new JResolutionResult<>(system.constants().voidType(), value);
            } else {
                return JResolutionResult.inherit(value, system.constants().allPrimitives().stream().filter(t -> t.namespace().location().equals(value)).findFirst(),
                        "Unknown primitive type");
            }
        }

        return new JResolutionResult<>("Not a primitive type", value);
    }

}
