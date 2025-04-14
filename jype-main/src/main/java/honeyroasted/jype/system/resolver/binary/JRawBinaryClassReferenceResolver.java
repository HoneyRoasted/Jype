package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.metadata.JClassBytecode;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JType;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;

public class JRawBinaryClassReferenceResolver implements JTypeResolver<JClassBytecode, JType> {
    @Override
    public JResolutionResult<JClassBytecode, JType> resolve(JTypeSystem system, JClassBytecode value) {
        ClassModel model = ClassFile.of().parse(value.bytes());
        return JResolutionResult.inherit(value, system.resolve(ClassModel.class, JType.class, model));
    }
}
