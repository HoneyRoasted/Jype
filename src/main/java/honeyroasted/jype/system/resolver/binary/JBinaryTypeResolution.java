package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.system.resolver.JBundledTypeResolvers;
import honeyroasted.jype.system.resolver.general.JDeclaredSignatureTypeResolver;

public interface JBinaryTypeResolution {

    JBundledTypeResolvers BINARY_TYPE_RESOLVERS = new JBundledTypeResolvers(
            new JDescClassReferenceResolver(),
            new JEntryClassReferenceResolver(),
            new JRawBinaryClassReferenceResolver(),

            new JModelClassReferenceResolver(),
            new JDeclaredSignatureTypeResolver()
    );
}
