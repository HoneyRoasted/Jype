package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.location.JClassBytecode;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JType;
import org.glavo.classfile.ClassFile;
import org.glavo.classfile.ClassModel;

import java.util.Optional;

public class JRawBinaryClassReferenceResolver implements JTypeResolver<JClassBytecode, JType> {

    @Override
    public Optional<? extends JType> resolve(JTypeSystem system, JClassBytecode value) {
        ClassModel model = ClassFile.of().parse(value.bytes());
        return system.resolve(ClassModel.class, JType.class, model);
    }
}
