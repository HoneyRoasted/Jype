package honeyroasted.jype.system;

import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.location.JClassNamespace;
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
import honeyroasted.jype.system.resolver.general.JGeneralTypeResolution;
import honeyroasted.jype.system.resolver.reflection.JReflectionTypeResolution;
import honeyroasted.jype.system.solver.operations.JTypeOperations;
import honeyroasted.jype.system.solver.operations.JTypeOperationsImpl;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JType;

import java.io.Serializable;
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
        this(cacheFactory, JGeneralTypeResolution.GENERAL_TYPE_RESOLVERS, JReflectionTypeResolution.REFLECTION_TYPE_RESOLVERS);
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
