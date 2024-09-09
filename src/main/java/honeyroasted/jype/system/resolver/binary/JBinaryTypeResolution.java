package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.system.resolver.JBundledTypeResolvers;

public interface JBinaryTypeResolution {

    JBundledTypeResolvers BINARY_TYPE_RESOLVERS = new JBundledTypeResolvers(
            new JEntryClassReferenceResolver(),
            new JRawBinaryClassReferenceResolver(),

            new JModelClassReferenceResolver()
    );

}
