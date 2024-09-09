package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JType;
import org.glavo.classfile.ClassModel;

public class JModelClassReferenceResolver implements JTypeResolver<ClassModel, JType> {

    @Override
    public JResolutionResult<ClassModel, JType> resolve(JTypeSystem system, ClassModel value) {
        return new JResolutionResult<>("Unimplemented", value);
    }

}
