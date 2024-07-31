package honeyroasted.jype.system.resolver;

import honeyroasted.jype.type.Type;

public interface TypeResolvers {
    <I, O extends Type> void register(TypeResolver<I, O> resolver);

    <I> void clear(Class<I> keyType);

    <I, O extends Type> TypeResolver<I, O> resolverFor(Class<I> keyType, Class<O> outputType);
}
