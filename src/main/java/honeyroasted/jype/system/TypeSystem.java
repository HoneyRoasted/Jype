package honeyroasted.jype.system;

import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.system.cache.TypeCacheFactory;
import honeyroasted.jype.system.cache.TypeStorage;
import honeyroasted.jype.system.resolver.BundledTypeResolvers;
import honeyroasted.jype.system.resolver.ResolutionAttemptFailedException;
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

import java.io.Serializable;
import java.lang.reflect.Executable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class TypeSystem {
    public static TypeSystem RUNTIME = new TypeSystem();

    private TypeStorage storage;
    private TypeResolvers resolvers;
    private TypeConstants constants;
    private TypeOperations operations;

    public TypeSystem() {
        this(TypeCacheFactory.IN_MEMORY_FACTORY);
    }

    public TypeSystem(TypeCacheFactory cacheFactory) {
        this(cacheFactory, ReflectionTypeResolution.REFLECTION_TYPE_RESOLVERS);
    }

    public TypeSystem(TypeResolver... initialResolvers) {
        this(TypeCacheFactory.IN_MEMORY_FACTORY, initialResolvers);
    }

    public TypeSystem(TypeCacheFactory cacheFactory, TypeResolver... initialResolvers) {
        this(cacheFactory, List.of(initialResolvers));
    }

    public TypeSystem(TypeCacheFactory cacheFactory, Collection<? extends TypeResolver> initialResolvers) {
        this.storage = new TypeStorage(cacheFactory);
        this.resolvers = new TypeResolvers();
        this.registerResolvers(initialResolvers);

        this.constants = new TypeConstants(
                this.tryLocResolve(Object.class), this.tryLocResolve(Cloneable.class), this.tryLocResolve(Serializable.class),
                this.tryLocResolve(RuntimeException.class),
                new NoneTypeImpl(this, "void"), new NoneTypeImpl(this, "null"), new NoneTypeImpl(this, "none"),
                this.tryLocResolve(Void.class),
                new PrimitiveTypeImpl(this, ClassNamespace.of(boolean.class), this.tryLocResolve(Boolean.class), "Z"),
                new PrimitiveTypeImpl(this, ClassNamespace.of(byte.class), this.tryLocResolve(Byte.class), "B"),
                new PrimitiveTypeImpl(this, ClassNamespace.of(short.class), this.tryLocResolve(Short.class), "S"),
                new PrimitiveTypeImpl(this, ClassNamespace.of(char.class), this.tryLocResolve(Character.class), "C"),
                new PrimitiveTypeImpl(this, ClassNamespace.of(int.class), this.tryLocResolve(Integer.class), "I"),
                new PrimitiveTypeImpl(this, ClassNamespace.of(long.class), this.tryLocResolve(Long.class), "J"),
                new PrimitiveTypeImpl(this, ClassNamespace.of(float.class), this.tryLocResolve(Float.class), "F"),
                new PrimitiveTypeImpl(this, ClassNamespace.of(double.class), this.tryLocResolve(Double.class), "D")
        );

        this.operations = new TypeOperations(this);
    }

    private ClassReference tryLocResolve(Class<?> cls) {
        return (ClassReference) this.resolve(ClassLocation.class, Type.class, ClassLocation.of(cls))
                .orElseThrow(() -> new ResolutionAttemptFailedException("Could not resolve " + cls.getName() + " type"));
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

    public TypeOperations operations() {
        return this.operations;
    }

    public <I, O extends Type> Optional<? extends O> resolve(Class<I> keyType, Class<O> resultType, I key) {
        return this.resolvers().resolverFor(keyType, resultType).resolve(this, key);
    }

    public <T extends Type> Optional<T> resolve(TypeToken<?> token) {
        return (Optional<T>) this.resolve(TypeToken.class, Type.class, token);
    }

    public <T extends Type> T tryResolve(TypeToken<?> token) {
        return this.<T>resolve(token).orElseThrow(() -> new ResolutionAttemptFailedException("Could not resolve " + token + " to a type"));
    }

    public <T extends Type> Optional<T> resolve(java.lang.reflect.Type reflectionType) {
        return (Optional<T>) this.resolve(java.lang.reflect.Type.class, Type.class, reflectionType);
    }

    public <T extends Type> T tryResolve(java.lang.reflect.Type reflectionType) {
        return this.<T>resolve(reflectionType).orElseThrow(() -> new ResolutionAttemptFailedException("Could not resolve " + reflectionType + " to a type"));
    }

    public <T extends Type> Optional<T> resolve(ClassLocation classLocation) {
        return (Optional<T>) this.resolve(ClassLocation.class, Type.class, classLocation);
    }

    public <T extends Type> T tryResolve(ClassLocation classLocation) {
        return this.<T>resolve(classLocation).orElseThrow(() -> new ResolutionAttemptFailedException("Could not resolve " + classLocation + " to a type"));
    }

    public <T extends MethodReference> Optional<T> resolve(Executable executable) {
        return (Optional<T>) this.resolve(Executable.class, MethodReference.class, executable);
    }

    public <T extends MethodReference> T tryResolve(Executable executable) {
        return this.<T>resolve(executable).orElseThrow(() -> new ResolutionAttemptFailedException("Could not resolve " + executable + " to a type"));
    }

    public<T extends MethodReference> Optional<T> resolve(MethodLocation methodLocation) {
        return (Optional<T>) this.resolve(MethodLocation.class, MethodReference.class, methodLocation);
    }

    public <T extends MethodReference> T tryResolve(MethodLocation methodLocation) {
        return this.<T>resolve(methodLocation).orElseThrow(() -> new ResolutionAttemptFailedException("Could not resolve " + methodLocation + " to a type"));
    }

    public <T extends VarType> Optional<T> resolve(TypeParameterLocation parameterLocation) {
        return (Optional<T>) this.resolve(TypeParameterLocation.class, VarType.class, parameterLocation);
    }

    public <T extends VarType> T tryResolve(TypeParameterLocation parameterLocation) {
        return this.<T>resolve(parameterLocation).orElseThrow(() -> new ResolutionAttemptFailedException("Could not resolve " + parameterLocation + " to a type"));
    }

    public void registerResolvers(TypeResolver... resolvers) {
        for (TypeResolver resolver : resolvers) {
            this.registerResolver(resolver);
        }
    }

    public void registerResolvers(Collection<? extends TypeResolver> resolvers) {
        for (TypeResolver resolver : resolvers) {
            this.registerResolver(resolver);
        }
    }

    public void registerResolver(TypeResolver resolver) {
        if (resolver instanceof BundledTypeResolvers bundle) {
            this.registerResolvers(bundle.resolvers());
        } else {
            this.resolvers().register(resolver);
        }
    }

}
