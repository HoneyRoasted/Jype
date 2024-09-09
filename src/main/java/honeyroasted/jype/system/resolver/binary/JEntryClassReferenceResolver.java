package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JType;
import org.glavo.classfile.constantpool.ClassEntry;

import java.util.Optional;

public class JEntryClassReferenceResolver implements JTypeResolver<ClassEntry, JType> {
    @Override
    public Optional<? extends JType> resolve(JTypeSystem system, ClassEntry value) {
        return system.resolve(JClassLocation.of(value.asInternalName()));
    }
}
