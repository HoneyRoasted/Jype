package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.metadata.location.JClassLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JType;
import java.lang.classfile.constantpool.ClassEntry;

public class JEntryClassReferenceResolver implements JTypeResolver<ClassEntry, JType> {
    @Override
    public JResolutionResult<ClassEntry, JType> resolve(JTypeSystem system, ClassEntry value) {
        return JResolutionResult.inherit(value, system.resolve(JClassLocation.of(value.asInternalName())));
    }
}
