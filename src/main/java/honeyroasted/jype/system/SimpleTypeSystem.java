package honeyroasted.jype.system;

import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.system.cache.InMemoryTypeStorage;
import honeyroasted.jype.system.cache.TypeCacheFactory;
import honeyroasted.jype.system.cache.TypeStorage;
import honeyroasted.jype.system.expression.ExpressionInspector;
import honeyroasted.jype.system.expression.ReflectionExpressionInspector;
import honeyroasted.jype.system.resolver.BundledTypeResolvers;
import honeyroasted.jype.system.resolver.InMemoryTypeResolvers;
import honeyroasted.jype.system.resolver.ResolutionAttemptFailedException;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.system.resolver.TypeResolvers;
import honeyroasted.jype.system.resolver.reflection.ReflectionTypeResolution;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.system.solver.operations.TypeOperations;
import honeyroasted.jype.system.solver.operations.TypeOperationsImpl;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.io.Serializable;
import java.lang.reflect.Executable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class SimpleTypeSystem implements TypeSystem {
    private TypeStorage storage;
    private TypeResolvers resolvers;
    private TypeConstants constants;
    private TypeOperations operations;
    private TypeFactory typeFactory;
    private ExpressionInspector expressionInspector;

    public SimpleTypeSystem() {
        this(TypeCacheFactory.IN_MEMORY_FACTORY);
    }

    public SimpleTypeSystem(TypeCacheFactory cacheFactory) {
        this(cacheFactory, ReflectionTypeResolution.REFLECTION_TYPE_RESOLVERS);
    }

    public SimpleTypeSystem(TypeResolver... initialResolvers) {
        this(TypeCacheFactory.IN_MEMORY_FACTORY, initialResolvers);
    }

    public SimpleTypeSystem(TypeCacheFactory cacheFactory, TypeResolver... initialResolvers) {
        this(cacheFactory, List.of(initialResolvers), SimpleTypeFactory::new, ReflectionExpressionInspector::new);
    }

    public SimpleTypeSystem(TypeCacheFactory cacheFactory, Collection<? extends TypeResolver> initialResolvers, Function<TypeSystem, TypeFactory> typeFactory, Function<TypeSystem, ExpressionInspector> expressionInspector) {
        this.storage = new InMemoryTypeStorage(cacheFactory);
        this.resolvers = new InMemoryTypeResolvers();
        this.registerResolvers(initialResolvers);
        this.typeFactory = typeFactory.apply(this);
        this.expressionInspector = expressionInspector.apply(this);

        this.constants = new InMemoryTypeConstants(
                this.tryLocResolve(Object.class), this.tryLocResolve(Cloneable.class), this.tryLocResolve(Serializable.class),
                this.tryLocResolve(RuntimeException.class),
                this.typeFactory().newNoneType("void"), this.typeFactory().newNoneType("null"), this.typeFactory().newNoneType("none"),
                this.tryLocResolve(Void.class),
                this.typeFactory().newPrimitiveType(ClassNamespace.of(boolean.class), this.tryLocResolve(Boolean.class), "Z"),
                this.typeFactory().newPrimitiveType(ClassNamespace.of(byte.class), this.tryLocResolve(Byte.class), "B"),
                this.typeFactory().newPrimitiveType(ClassNamespace.of(short.class), this.tryLocResolve(Short.class), "S"),
                this.typeFactory().newPrimitiveType(ClassNamespace.of(char.class), this.tryLocResolve(Character.class), "C"),
                this.typeFactory().newPrimitiveType(ClassNamespace.of(int.class), this.tryLocResolve(Integer.class), "I"),
                this.typeFactory().newPrimitiveType(ClassNamespace.of(long.class), this.tryLocResolve(Long.class), "J"),
                this.typeFactory().newPrimitiveType(ClassNamespace.of(float.class), this.tryLocResolve(Float.class), "F"),
                this.typeFactory().newPrimitiveType(ClassNamespace.of(double.class), this.tryLocResolve(Double.class), "D")
        );

        this.operations = new TypeOperationsImpl(this);
    }

    private ClassReference tryLocResolve(Class<?> cls) {
        return (ClassReference) this.resolve(ClassLocation.class, Type.class, ClassLocation.of(cls))
                .orElseThrow(() -> new ResolutionAttemptFailedException("Could not resolve " + cls.getName() + " type"));
    }

    @Override
    public TypeConstants constants() {
        return this.constants;
    }

    @Override
    public TypeStorage storage() {
        return this.storage;
    }

    @Override
    public TypeResolvers resolvers() {
        return this.resolvers;
    }

    @Override
    public TypeOperations operations() {
        return this.operations;
    }

    @Override
    public <I, O extends Type> Optional<? extends O> resolve(Class<I> keyType, Class<O> resultType, I key) {
        return this.resolvers().resolverFor(keyType, resultType).resolve(this, key);
    }

    @Override
    public <T extends Type> Optional<T> resolve(TypeToken<?> token) {
        return (Optional<T>) this.resolve(TypeToken.class, Type.class, token);
    }

    @Override
    public <T extends Type> T tryResolve(TypeToken<?> token) {
        return this.<T>resolve(token).orElseThrow(() -> new ResolutionAttemptFailedException("Could not resolve " + token + " to a type"));
    }

    @Override
    public <T extends Type> Optional<T> resolve(java.lang.reflect.Type reflectionType) {
        return (Optional<T>) this.resolve(java.lang.reflect.Type.class, Type.class, reflectionType);
    }

    @Override
    public <T extends Type> T tryResolve(java.lang.reflect.Type reflectionType) {
        return this.<T>resolve(reflectionType).orElseThrow(() -> new ResolutionAttemptFailedException("Could not resolve " + reflectionType + " to a type"));
    }

    @Override
    public <T extends Type> Optional<T> resolve(ClassLocation classLocation) {
        return (Optional<T>) this.resolve(ClassLocation.class, Type.class, classLocation);
    }

    @Override
    public <T extends Type> T tryResolve(ClassLocation classLocation) {
        return this.<T>resolve(classLocation).orElseThrow(() -> new ResolutionAttemptFailedException("Could not resolve " + classLocation + " to a type"));
    }

    @Override
    public <T extends MethodReference> Optional<T> resolve(Executable executable) {
        return (Optional<T>) this.resolve(Executable.class, MethodReference.class, executable);
    }

    @Override
    public <T extends MethodReference> T tryResolve(Executable executable) {
        return this.<T>resolve(executable).orElseThrow(() -> new ResolutionAttemptFailedException("Could not resolve " + executable + " to a type"));
    }

    @Override
    public<T extends MethodReference> Optional<T> resolve(MethodLocation methodLocation) {
        return (Optional<T>) this.resolve(MethodLocation.class, MethodReference.class, methodLocation);
    }

    @Override
    public <T extends MethodReference> T tryResolve(MethodLocation methodLocation) {
        return this.<T>resolve(methodLocation).orElseThrow(() -> new ResolutionAttemptFailedException("Could not resolve " + methodLocation + " to a type"));
    }

    @Override
    public <T extends VarType> Optional<T> resolve(TypeParameterLocation parameterLocation) {
        return (Optional<T>) this.resolve(TypeParameterLocation.class, VarType.class, parameterLocation);
    }

    @Override
    public <T extends VarType> T tryResolve(TypeParameterLocation parameterLocation) {
        return this.<T>resolve(parameterLocation).orElseThrow(() -> new ResolutionAttemptFailedException("Could not resolve " + parameterLocation + " to a type"));
    }

    @Override
    public void registerResolvers(TypeResolver... resolvers) {
        for (TypeResolver resolver : resolvers) {
            this.registerResolver(resolver);
        }
    }

    @Override
    public void registerResolvers(Collection<? extends TypeResolver> resolvers) {
        for (TypeResolver resolver : resolvers) {
            this.registerResolver(resolver);
        }
    }

    @Override
    public void registerResolver(TypeResolver resolver) {
        if (resolver instanceof BundledTypeResolvers bundle) {
            this.registerResolvers(bundle.resolvers());
        } else {
            this.resolvers().register(resolver);
        }
    }

    @Override
    public TypeFactory typeFactory() {
        return this.typeFactory;
    }

    @Override
    public ExpressionInspector expressionInspector() {
        return this.expressionInspector;
    }


}
