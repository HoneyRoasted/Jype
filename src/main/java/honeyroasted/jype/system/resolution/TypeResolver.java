package honeyroasted.jype.system.resolution;

import honeyroasted.jype.Type;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.TypeDeclaration;

public interface TypeResolver<T, K> {

    Type resolve(T type);

    TypeDeclaration resolveDeclaration(K type);

    TypeSystem typeSystem();

    TypeCache<T> cache();

    Class<T> typeClass();

    Class<K> declarationClass();

}
