package honeyroasted.jype.system.resolver;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.Type;

import java.util.Optional;

public interface TypeResolver<I, O extends Type> {

    Optional<O> resolve(TypeSystem system, TypeCache<I, O> cache, I value);

    Class<I> inputType();

    Class<O> outputType();

}
