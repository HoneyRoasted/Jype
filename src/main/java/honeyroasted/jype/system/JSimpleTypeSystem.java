package honeyroasted.jype.system;

import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.location.JClassNamespace;
import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.location.JTypeParameterLocation;
import honeyroasted.jype.system.cache.JInMemoryTypeStorage;
import honeyroasted.jype.system.cache.JTypeCacheFactory;
import honeyroasted.jype.system.cache.JTypeStorage;
import honeyroasted.jype.system.expression.JExpressionInspector;
import honeyroasted.jype.system.expression.JReflectionExpressionInspector;
import honeyroasted.jype.system.resolver.JBundledTypeResolvers;
import honeyroasted.jype.system.resolver.JInMemoryTypeResolvers;
import honeyroasted.jype.system.resolver.JResolutionAttemptFailedException;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.system.resolver.JTypeResolvers;
import honeyroasted.jype.system.resolver.reflection.JReflectionTypeResolution;
import honeyroasted.jype.system.resolver.reflection.JTypeToken;
import honeyroasted.jype.system.solver.operations.JTypeOperations;
import honeyroasted.jype.system.solver.operations.JTypeOperationsImpl;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.io.Serializable;
import java.lang.reflect.Executable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class JSimpleTypeSystem implements JTypeSystem {
    private JTypeStorage storage;
    private JTypeResolvers resolvers;
    private JTypeConstants constants;
    private JTypeOperations operations;
    private JTypeFactory typeFactory;
    private JExpressionInspector expressionInspector;

    public JSimpleTypeSystem() {
        this(JTypeCacheFactory.IN_MEMORY_FACTORY);
    }

    public JSimpleTypeSystem(JTypeCacheFactory cacheFactory) {
        this(cacheFactory, JReflectionTypeResolution.REFLECTION_TYPE_RESOLVERS);
    }

    public JSimpleTypeSystem(JTypeResolver... initialResolvers) {
        this(JTypeCacheFactory.IN_MEMORY_FACTORY, initialResolvers);
    }

    public JSimpleTypeSystem(JTypeCacheFactory cacheFactory, JTypeResolver... initialResolvers) {
        this(cacheFactory, List.of(initialResolvers), JSimpleTypeFactory::new, JReflectionExpressionInspector::new);
    }

    public JSimpleTypeSystem(JTypeCacheFactory cacheFactory, Collection<? extends JTypeResolver> initialResolvers, Function<JTypeSystem, JTypeFactory> typeFactory, Function<JTypeSystem, JExpressionInspector> expressionInspector) {
        this.storage = new JInMemoryTypeStorage(cacheFactory);
        this.resolvers = new JInMemoryTypeResolvers();
        this.registerResolvers(initialResolvers);
        this.typeFactory = typeFactory.apply(this);
        this.expressionInspector = expressionInspector.apply(this);

        this.constants = new JInMemoryTypeConstants(
                this.tryLocResolve(Object.class), this.tryLocResolve(Cloneable.class), this.tryLocResolve(Serializable.class),
                this.tryLocResolve(RuntimeException.class),
                this.typeFactory().newNoneType("void"), this.typeFactory().newNoneType("null"), this.typeFactory().newNoneType("none"),
                this.tryLocResolve(Void.class),
                this.typeFactory().newPrimitiveType(JClassNamespace.of(boolean.class), this.tryLocResolve(Boolean.class), "Z"),
                this.typeFactory().newPrimitiveType(JClassNamespace.of(byte.class), this.tryLocResolve(Byte.class), "B"),
                this.typeFactory().newPrimitiveType(JClassNamespace.of(short.class), this.tryLocResolve(Short.class), "S"),
                this.typeFactory().newPrimitiveType(JClassNamespace.of(char.class), this.tryLocResolve(Character.class), "C"),
                this.typeFactory().newPrimitiveType(JClassNamespace.of(int.class), this.tryLocResolve(Integer.class), "I"),
                this.typeFactory().newPrimitiveType(JClassNamespace.of(long.class), this.tryLocResolve(Long.class), "J"),
                this.typeFactory().newPrimitiveType(JClassNamespace.of(float.class), this.tryLocResolve(Float.class), "F"),
                this.typeFactory().newPrimitiveType(JClassNamespace.of(double.class), this.tryLocResolve(Double.class), "D")
        );

        this.operations = new JTypeOperationsImpl(this);
    }

    private JClassReference tryLocResolve(Class<?> cls) {
        return (JClassReference) this.resolve(JClassLocation.class, JType.class, JClassLocation.of(cls))
                .orElseThrow(() -> new JResolutionAttemptFailedException("Could not resolve " + cls.getName() + " type"));
    }

    @Override
    public JTypeConstants constants() {
        return this.constants;
    }

    @Override
    public JTypeStorage storage() {
        return this.storage;
    }

    @Override
    public JTypeResolvers resolvers() {
        return this.resolvers;
    }

    @Override
    public JTypeOperations operations() {
        return this.operations;
    }

    @Override
    public <I, O extends JType> Optional<? extends O> resolve(Class<I> keyType, Class<O> resultType, I key) {
        return this.resolvers().resolverFor(keyType, resultType).resolve(this, key);
    }

    @Override
    public <T extends JType> Optional<T> resolve(JTypeToken<?> token) {
        return (Optional<T>) this.resolve(JTypeToken.class, JType.class, token);
    }

    @Override
    public <T extends JType> T tryResolve(JTypeToken<?> token) {
        return this.<T>resolve(token).orElseThrow(() -> new JResolutionAttemptFailedException("Could not resolve " + token + " to a type"));
    }

    @Override
    public <T extends JType> Optional<T> resolve(java.lang.reflect.Type reflectionType) {
        return (Optional<T>) this.resolve(java.lang.reflect.Type.class, JType.class, reflectionType);
    }

    @Override
    public <T extends JType> T tryResolve(java.lang.reflect.Type reflectionType) {
        return this.<T>resolve(reflectionType).orElseThrow(() -> new JResolutionAttemptFailedException("Could not resolve " + reflectionType + " to a type"));
    }

    @Override
    public <T extends JType> Optional<T> resolve(JClassLocation classLocation) {
        return (Optional<T>) this.resolve(JClassLocation.class, JType.class, classLocation);
    }

    @Override
    public <T extends JType> T tryResolve(JClassLocation classLocation) {
        return this.<T>resolve(classLocation).orElseThrow(() -> new JResolutionAttemptFailedException("Could not resolve " + classLocation + " to a type"));
    }

    @Override
    public <T extends JMethodReference> Optional<T> resolve(Executable executable) {
        return (Optional<T>) this.resolve(Executable.class, JMethodReference.class, executable);
    }

    @Override
    public <T extends JMethodReference> T tryResolve(Executable executable) {
        return this.<T>resolve(executable).orElseThrow(() -> new JResolutionAttemptFailedException("Could not resolve " + executable + " to a type"));
    }

    @Override
    public <T extends JMethodReference> Optional<T> resolve(JMethodLocation methodLocation) {
        return (Optional<T>) this.resolve(JMethodLocation.class, JMethodReference.class, methodLocation);
    }

    @Override
    public <T extends JMethodReference> T tryResolve(JMethodLocation methodLocation) {
        return this.<T>resolve(methodLocation).orElseThrow(() -> new JResolutionAttemptFailedException("Could not resolve " + methodLocation + " to a type"));
    }

    @Override
    public <T extends JVarType> Optional<T> resolve(JTypeParameterLocation parameterLocation) {
        return (Optional<T>) this.resolve(JTypeParameterLocation.class, JVarType.class, parameterLocation);
    }

    @Override
    public <T extends JVarType> T tryResolve(JTypeParameterLocation parameterLocation) {
        return this.<T>resolve(parameterLocation).orElseThrow(() -> new JResolutionAttemptFailedException("Could not resolve " + parameterLocation + " to a type"));
    }

    @Override
    public void registerResolvers(JTypeResolver... resolvers) {
        for (JTypeResolver resolver : resolvers) {
            this.registerResolver(resolver);
        }
    }

    @Override
    public void registerResolvers(Collection<? extends JTypeResolver> resolvers) {
        for (JTypeResolver resolver : resolvers) {
            this.registerResolver(resolver);
        }
    }

    @Override
    public void registerResolver(JTypeResolver resolver) {
        if (resolver instanceof JBundledTypeResolvers bundle) {
            this.registerResolvers(bundle.resolvers());
        } else {
            this.resolvers().register(resolver);
        }
    }

    @Override
    public JTypeFactory typeFactory() {
        return this.typeFactory;
    }

    @Override
    public JExpressionInspector expressionInspector() {
        return this.expressionInspector;
    }


}
