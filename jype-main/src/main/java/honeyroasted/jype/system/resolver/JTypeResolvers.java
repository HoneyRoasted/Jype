package honeyroasted.jype.system.resolver;

import honeyroasted.jype.type.JType;

public interface JTypeResolvers {
    <I, O extends JType> void register(JTypeResolver<I, O> resolver);

    <I> void clear(Class<I> keyType);

    <I, O extends JType> JTypeResolver<I, O> resolverFor(Class<I> keyType, Class<O> outputType);
}
