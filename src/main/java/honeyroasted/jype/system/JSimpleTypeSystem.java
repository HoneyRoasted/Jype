package honeyroasted.jype.system;

import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.location.JClassNamespace;
import honeyroasted.jype.system.cache.JInMemoryTypeStorage;
import honeyroasted.jype.system.cache.JTypeCacheFactory;
import honeyroasted.jype.system.cache.JTypeStorage;
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
        this(cacheFactory, List.of(initialResolvers), JSimpleTypeFactory::new);
    }

    public JSimpleTypeSystem(JTypeCacheFactory cacheFactory, Collection<? extends JTypeResolver> initialResolvers, Function<JTypeSystem, JTypeFactory> typeFactory) {
        this.storage = new JInMemoryTypeStorage(cacheFactory);
        this.resolvers = new JInMemoryTypeResolvers();
        this.registerResolvers(initialResolvers);
        this.typeFactory = typeFactory.apply(this);

        JInMemoryTypeConstants constants = new JInMemoryTypeConstants();
        this.constants = constants;

        constants.setVoidType(this.typeFactory.newNoneType("void"))
                .setNullType(this.typeFactory.newNoneType("null"))
                .setNoneType(this.typeFactory.newNoneType("none"));

        constants.setBooleanType(this.typeFactory.newPrimitiveType(JClassNamespace.of(boolean.class), JClassNamespace.of(Boolean.class), "Z"))
                .setByteType(this.typeFactory.newPrimitiveType(JClassNamespace.of(byte.class), JClassNamespace.of(Byte.class), "B"))
                .setShortType(this.typeFactory.newPrimitiveType(JClassNamespace.of(short.class), JClassNamespace.of(Short.class), "S"))
                .setCharType(this.typeFactory.newPrimitiveType(JClassNamespace.of(char.class), JClassNamespace.of(Character.class), "C"))
                .setIntType(this.typeFactory.newPrimitiveType(JClassNamespace.of(int.class), JClassNamespace.of(Integer.class), "I"))
                .setLongType(this.typeFactory.newPrimitiveType(JClassNamespace.of(long.class), JClassNamespace.of(Long.class), "J"))
                .setFloatType(this.typeFactory.newPrimitiveType(JClassNamespace.of(float.class), JClassNamespace.of(Float.class), "F"))
                .setDoubleType(this.typeFactory.newPrimitiveType(JClassNamespace.of(double.class), JClassNamespace.of(Double.class), "D"))
                .initPrimitiveMaps();

        constants.setObject(this.tryLocResolve(Object.class))
                .setCloneable(this.tryLocResolve(Cloneable.class))
                .setSerializable(this.tryLocResolve(Serializable.class))
                .setRuntimeException(this.tryLocResolve(RuntimeException.class));

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
}
