package honeyroasted.jype.system.resolution;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.TypeDeclaration;

public interface TypeResolver<T, K> {

    TypeConcrete resolve(T type);

    TypeDeclaration resolveDeclaration(K type);

    TypeSystem typeSystem();

    TypeCache<T> cache();

    boolean acceptsType(Object type);

    boolean acceptsDeclaration(Object type);

}
