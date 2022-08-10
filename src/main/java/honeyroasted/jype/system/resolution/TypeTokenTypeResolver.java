package honeyroasted.jype.system.resolution;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.TypeToken;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.TypeDeclaration;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * This is a {@link TypeResolver} for resolving {@link honeyroasted.jype.Type}s from the utility class {@link TypeToken}.
 * It is backed by a {@link ReflectionTypeResolver} and passes the reified generic information from {@link TypeToken}
 * to the backing resolver.
 */
public class TypeTokenTypeResolver implements TypeResolver<TypeToken, TypeToken> {
    private ReflectionTypeResolver backer;

    /**
     * Creates a new {@link TypeTokenTypeResolver}.
     *
     * @param typeSystem The {@link TypeSystem} this {@link TypeTokenTypeResolver} is resolving types for
     * @param cache      The {@link TypeCache} this {@link TypeTokenTypeResolver} is using to cache resolved types
     */
    public TypeTokenTypeResolver(TypeSystem typeSystem, TypeCache<? super Type> cache) {
        this.backer = new ReflectionTypeResolver(typeSystem, cache);
    }

    @Override
    public TypeConcrete resolve(TypeToken type) {
        return this.backer.resolve(((ParameterizedType) type.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    @Override
    public TypeDeclaration resolveDeclaration(TypeToken type) {
        return this.backer.resolveDeclaration((Class) ((ParameterizedType) type.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    @Override
    public boolean acceptsType(Object type) {
        return type instanceof TypeToken<?> && type.getClass().getGenericSuperclass() instanceof ParameterizedType pt &&
                pt.getActualTypeArguments().length == 1;
    }

    @Override
    public boolean acceptsDeclaration(Object type) {
        return type instanceof TypeToken<?> && type.getClass().getGenericSuperclass() instanceof ParameterizedType pt &&
                pt.getActualTypeArguments().length == 1 && pt.getActualTypeArguments()[0] instanceof Class<?>;
    }
}
