package honeyroasted.jype.system.resolution;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.TypeDeclaration;

/**
 * This is a utility class that handles common code for implementations of {@link TypeResolver}, including holding a
 * reference to the {@link TypeSystem} and a {@link TypeCache}, as well as providing default implementations of
 * {@link TypeResolver#acceptsType(Object)} and {@link TypeResolver#acceptsDeclaration(Object)}.
 *
 * @param <T> The type of objects that can be transformed into {@link TypeConcrete}s
 * @param <K> The type of objects that can be transformed into {@link TypeDeclaration}s
 */
public abstract class AbstractTypeResolver<T, K> implements TypeResolver<T, K> {
    private TypeSystem typeSystem;
    private TypeCache<? super T> cache;

    private Class<T> typeClass;
    private Class<K> declarationClass;

    /**
     * Creates a new {@link AbstractTypeResolver}.
     *
     * @param typeSystem       The {@link TypeSystem} this {@link TypeResolver} is resolving types for
     * @param cache            The {@link TypeCache} this {@link TypeResolver} is using to cache resolved types
     * @param typeClass        The class corresponding to objects that this {@link TypeResolver} can transform into {@link TypeConcrete}s
     * @param declarationClass The class corresponding to objects that this {@link TypeResolver} can transform into {@link TypeDeclaration}s
     */
    public AbstractTypeResolver(TypeSystem typeSystem, TypeCache<? super T> cache, Class<T> typeClass, Class<K> declarationClass) {
        this.typeSystem = typeSystem;
        this.cache = cache;
        this.typeClass = typeClass;
        this.declarationClass = declarationClass;
    }

    /**
     * @return The {@link TypeSystem} this {@link TypeResolver} is resolving types for
     */
    public TypeSystem typeSystem() {
        return this.typeSystem;
    }

    /**
     * @return The {@link TypeCache} this {@link TypeResolver} is using to cache resolved types
     */
    public TypeCache<? super T> cache() {
        return this.cache;
    }

    @Override
    public boolean acceptsType(Object type) {
        return this.typeClass.isInstance(type);
    }

    @Override
    public boolean acceptsDeclaration(Object type) {
        return this.declarationClass.isInstance(type);
    }
}
