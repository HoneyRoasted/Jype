package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.metadata.location.JClassLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JType;

import java.lang.constant.ClassDesc;

public class JDescClassReferenceResolver implements JTypeResolver<ClassDesc, JType> {
    @Override
    public JResolutionResult<ClassDesc, JType> resolve(JTypeSystem system, ClassDesc value) {
        String desc = value.descriptorString();
        if (value.isPrimitive()) {
            return JResolutionResult.inherit(value, system.constants().allPrimitives().stream().filter(prim -> prim.descriptor().equals(desc)).findFirst(),
                    "Unknown primitive descriptor");
        } else if (value.isArray()) {
            return system.resolve(ClassDesc.class, JType.class, value.componentType()).map(value, comp -> {
                JArrayType arr = system.typeFactory().newArrayType();
                arr.setComponent(comp);
                arr.setUnmodifiable(true);
                return arr;
            }, "Failed to create array type");
        } else if (value.isClassOrInterface()) {
            return JResolutionResult.inherit(value, system.resolve(JClassLocation.of(desc.substring(1, desc.length() - 1))));
        }

        return new JResolutionResult<>("Unknown descriptor type", value);
    }
}
