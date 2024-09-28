package honeyroasted.jype.system.resolver.general;

import honeyroasted.jype.system.resolver.JBundledTypeResolvers;

public interface JGeneralTypeResolution {

    JBundledTypeResolvers GENERAL_TYPE_RESOLVERS = new JBundledTypeResolvers(
            new JClassSourceNameResolver(),
            new JTypeParameterLocationResolver(),
            new JLocationPrimitiveResolver()
    );

}
