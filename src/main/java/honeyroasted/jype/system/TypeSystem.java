package honeyroasted.jype.system;

import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.system.cache.InMemoryTypeCache;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.cache.TypeCacheFactory;
import honeyroasted.jype.system.cache.TypeStorage;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.system.resolver.TypeResolvers;
import honeyroasted.jype.system.resolver.reflection.*;
import honeyroasted.jype.type.*;

import java.util.Optional;

public class TypeSystem {
    private TypeStorage storage;
    private TypeResolvers resolvers;
    private TypeConstants constants;

    public TypeSystem(TypeCacheFactory cacheFactory) {
        this.storage = new TypeStorage(cacheFactory);
        this.resolvers = new TypeResolvers();

        this.registerResolvers(new ReflectionTypeTokenResolver(),
                new ReflectionTypeParameterResolver(),
                new ReflectionClassReferenceResolver(),
                new ReflectionJavaTypeResolver());

        this.constants = new TypeConstants(
                this.resolve(ClassLocation.class, ClassReference.class, ClassLocation.of(Object.class)).orElseThrow(() -> new IllegalStateException("Could not resolve java.lang.Object type")),
                new NoneType(this, "void"), new NoneType(this, "null"),
                new NoneType(this, "none"), new NoneType(this, "error"),
                new PrimitiveType(this, ClassNamespace.of(boolean.class), ClassNamespace.of(Boolean.class)),
                new PrimitiveType(this, ClassNamespace.of(byte.class), ClassNamespace.of(Byte.class)),
                new PrimitiveType(this, ClassNamespace.of(short.class), ClassNamespace.of(Short.class)),
                new PrimitiveType(this, ClassNamespace.of(char.class), ClassNamespace.of(Character.class)),
                new PrimitiveType(this, ClassNamespace.of(int.class), ClassNamespace.of(Integer.class)),
                new PrimitiveType(this, ClassNamespace.of(long.class), ClassNamespace.of(Long.class)),
                new PrimitiveType(this, ClassNamespace.of(float.class), ClassNamespace.of(Float.class)),
                new PrimitiveType(this, ClassNamespace.of(double.class), ClassNamespace.of(Double.class))
        );
    }

    public TypeSystem() {
        this(new TypeCacheFactory() {
            @Override
            public <K, T extends Type> TypeCache<K, T> createCache(Class<K> keyType) {
                return new InMemoryTypeCache<>();
            }
        });
    }

    public TypeConstants constants() {
        return this.constants;
    }

    public TypeStorage storage() {
        return this.storage;
    }

    public TypeResolvers resolvers() {
        return this.resolvers;
    }

    public <I, O extends Type> Optional<? extends O> resolve(Class<I> keyType, Class<O> resultType, I key) {
        return this.resolvers().resolverFor(keyType, resultType).resolve(this, key);
    }

    public Optional<? extends Type> resolve(TypeToken token) {
        return this.resolve(TypeToken.class, Type.class, token);
    }

    public Optional<? extends Type> resolve(java.lang.reflect.Type reflectionType) {
        return this.resolve(java.lang.reflect.Type.class, Type.class, reflectionType);
    }

    public Optional<? extends ClassReference> resolve(ClassLocation classLocation) {
        return this.resolve(ClassLocation.class, ClassReference.class, classLocation);
    }

    public Optional<? extends VarType> resolve(TypeParameterLocation parameterLocation) {
        return this.resolve(TypeParameterLocation.class, VarType.class, parameterLocation);
    }

    public void registerResolvers(TypeResolver... resolvers) {
        for (TypeResolver resolver : resolvers) {
            this.resolvers().register(resolver);
        }
    }
}
