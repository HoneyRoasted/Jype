package honeyroasted.jypestub.resolver;

import honeyroasted.jype.metadata.location.JClassLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JType;
import honeyroasted.jypestub.model.JStubFile;
import honeyroasted.jypestub.model.JStubFolder;
import honeyroasted.jypestub.model.types.JStubClass;

public class JStubLocationClassReferenceResolver implements JTypeResolver<JClassLocation, JType> {
    private JStubFolder folder;

    public JStubLocationClassReferenceResolver(JStubFolder folder) {
        this.folder = folder;
    }

    @Override
    public JResolutionResult<JClassLocation, JType> resolve(JTypeSystem system, JClassLocation value) {
        for (JStubFile file : this.folder.files()) {
            for (JStubClass cls : file.classes().values()) {
                if (cls.location().equals(value.toInternalName())) {
                    return JResolutionResult.inherit(value, system.resolve(JStubClass.class, JType.class, cls));
                }
            }
        }

        return new JResolutionResult<>("Could not find class stub (entry does not exist)", value);
    }
}
