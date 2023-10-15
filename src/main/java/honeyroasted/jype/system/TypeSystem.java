package honeyroasted.jype.system;

import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.system.cache.InMemoryTypeCache;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.cache.TypeCacheFactory;
import honeyroasted.jype.system.cache.TypeStorage;
import honeyroasted.jype.system.resolver.BundledTypeResolvers;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.system.resolver.TypeResolvers;
import honeyroasted.jype.system.resolver.reflection.ReflectionTypeResolution;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.impl.NoneTypeImpl;
import honeyroasted.jype.type.impl.PrimitiveTypeImpl;

import java.lang.reflect.Executable;
import java.util.Optional;

public class TypeSystem {
    public static TypeSystem RUNTIME = new TypeSystem();

    private TypeStorage storage;
    private TypeResolvers resolvers;
    private TypeConstants constants;

    public TypeSystem(TypeCacheFactory cacheFactory) {
        this(cacheFactory, ReflectionTypeResolution.REFLECTION_TYPE_RESOLVERS);
    }

    public TypeSystem() {
        this(new TypeCacheFactory() {
            @Override
            public <K, T extends Type> TypeCache<K, T> createCache(Class<K> keyType) {
                return new InMemoryTypeCache<>();
            }
        });
    }

    public TypeSystem(TypeCacheFactory cacheFactory, BundledTypeResolvers initialResolvers) {
        this.storage = new TypeStorage(cacheFactory);
        this.resolvers = new TypeResolvers();
        this.registerResolvers(initialResolvers);

        this.constants = new TypeConstants(
                this.tryResolve(Object.class),
                new NoneTypeImpl(this, "void"), new NoneTypeImpl(this, "null"), new NoneTypeImpl(this, "none"),
                this.tryResolve(Void.class),
                new PrimitiveTypeImpl(this, ClassNamespace.of(boolean.class), this.tryResolve(Boolean.class), "Z"),
                new PrimitiveTypeImpl(this, ClassNamespace.of(byte.class), this.tryResolve(Byte.class), "B"),
                new PrimitiveTypeImpl(this, ClassNamespace.of(short.class), this.tryResolve(Short.class), "S"),
                new PrimitiveTypeImpl(this, ClassNamespace.of(char.class), this.tryResolve(Character.class), "C"),
                new PrimitiveTypeImpl(this, ClassNamespace.of(int.class), this.tryResolve(Integer.class), "I"),
                new PrimitiveTypeImpl(this, ClassNamespace.of(long.class), this.tryResolve(Long.class), "J"),
                new PrimitiveTypeImpl(this, ClassNamespace.of(float.class), this.tryResolve(Float.class), "F"),
                new PrimitiveTypeImpl(this, ClassNamespace.of(double.class), this.tryResolve(Double.class), "D")
        );
    }

    private ClassReference tryResolve(Class<?> cls) {
        return (ClassReference) this.resolve(ClassLocation.class, Type.class, ClassLocation.of(cls))
                .orElseThrow(() -> new IllegalStateException("Could not resolve " + cls.getName() + " type"));
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

    public <T extends Type> Optional<T> resolve(TypeToken<?> token) {
        return (Optional<T>) this.resolve(TypeToken.class, Type.class, token);
    }

    public Optional<? extends Type> resolve(java.lang.reflect.Type reflectionType) {
        return this.resolve(java.lang.reflect.Type.class, Type.class, reflectionType);
    }

    public Optional<? extends Type> resolve(ClassLocation classLocation) {
        return this.resolve(ClassLocation.class, Type.class, classLocation);
    }

    public Optional<? extends MethodReference> resolve(Executable executable) {
        return this.resolve(Executable.class, MethodReference.class, executable);
    }

    public Optional<? extends MethodReference> resolve(MethodLocation methodLocation) {
        return this.resolve(MethodLocation.class, MethodReference.class, methodLocation);
    }

    public Optional<? extends VarType> resolve(TypeParameterLocation parameterLocation) {
        return this.resolve(TypeParameterLocation.class, VarType.class, parameterLocation);
    }

    public void registerResolvers(TypeResolver... resolvers) {
        for (TypeResolver resolver : resolvers) {
            this.resolvers().register(resolver);
        }
    }

    public void registerResolvers(BundledTypeResolvers bundle) {
        bundle.resolvers().forEach(this.resolvers()::register);
    }

}
