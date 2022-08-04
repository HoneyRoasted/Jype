package honeyroasted.jype.system.resolution.impl;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.resolution.TypeResolver;

public abstract class AbstractTypeResolver<T, K> implements TypeResolver<T, K> {
    private TypeSystem typeSystem;
    private TypeCache<T> cache;

    private Class<T> typeClass;
    private Class<K> declarationClass;

    public AbstractTypeResolver(TypeSystem typeSystem, TypeCache<T> cache, Class<T> typeClass, Class<K> declarationClass) {
        this.typeSystem = typeSystem;
        this.cache = cache;
        this.typeClass = typeClass;
        this.declarationClass = declarationClass;
    }

    @Override
    public TypeSystem typeSystem() {
        return this.typeSystem;
    }

    @Override
    public TypeCache<T> cache() {
        return this.cache;
    }

    @Override
    public Class<T> typeClass() {
        return this.typeClass;
    }

    @Override
    public Class<K> declarationClass() {
        return this.declarationClass;
    }
}
