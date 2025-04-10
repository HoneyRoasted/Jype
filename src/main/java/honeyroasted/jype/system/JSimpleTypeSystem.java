package honeyroasted.jype.system;

import honeyroasted.jype.metadata.location.JClassLocation;
import honeyroasted.jype.metadata.location.JClassNamespace;
import honeyroasted.jype.metadata.signature.JDescriptor;
import honeyroasted.jype.system.cache.JInMemoryTypeStorage;
import honeyroasted.jype.system.cache.JTypeCacheFactory;
import honeyroasted.jype.system.cache.JTypeStorage;
import honeyroasted.jype.system.resolver.JBundledTypeResolvers;
import honeyroasted.jype.system.resolver.JInMemoryTypeResolvers;
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
import java.util.function.Function;

public class JSimpleTypeSystem implements JTypeSystem {
    private String name;

    private JTypeStorage storage;
    private JTypeResolvers resolvers;
    private JTypeConstants constants;
    private JTypeOperations operations;
    private JTypeFactory typeFactory;

    public JSimpleTypeSystem(String name) {
        this(name, JTypeCacheFactory.IN_MEMORY_FACTORY);
    }

    public JSimpleTypeSystem(String name, JTypeCacheFactory cacheFactory) {
        this(name, cacheFactory, JReflectionTypeResolution.REFLECTION_TYPE_RESOLVERS, JGeneralTypeResolution.GENERAL_TYPE_RESOLVERS);
    }

    public JSimpleTypeSystem(String name, JTypeResolver... initialResolvers) {
        this(name, JTypeCacheFactory.IN_MEMORY_FACTORY, initialResolvers);
    }

    public JSimpleTypeSystem(String name, JTypeCacheFactory cacheFactory, JTypeResolver... initialResolvers) {
        this(name, cacheFactory, List.of(initialResolvers), JSimpleTypeFactory::new);
    }

    public JSimpleTypeSystem(String name, JTypeCacheFactory cacheFactory, Collection<? extends JTypeResolver> initialResolvers, Function<JTypeSystem, JTypeFactory> typeFactory) {
        this.name = name;
        this.storage = new JInMemoryTypeStorage(cacheFactory);
        this.resolvers = new JInMemoryTypeResolvers();
        this.registerResolvers(initialResolvers);
        this.typeFactory = typeFactory.apply(this);

        JInMemoryTypeConstants constants = new JInMemoryTypeConstants();
        this.constants = constants;

        constants.setVoidType(this.typeFactory.newNoneType("void"))
                .setNullType(this.typeFactory.newNoneType("null"))
                .setNoneType(this.typeFactory.newNoneType("none"));

        constants.setBooleanType(this.typeFactory.newPrimitiveType(JClassNamespace.of(boolean.class), JClassNamespace.of(Boolean.class), JDescriptor.Primitive.BOOLEAN))
                .setByteType(this.typeFactory.newPrimitiveType(JClassNamespace.of(byte.class), JClassNamespace.of(Byte.class), JDescriptor.Primitive.BYTE))
                .setShortType(this.typeFactory.newPrimitiveType(JClassNamespace.of(short.class), JClassNamespace.of(Short.class), JDescriptor.Primitive.SHORT))
                .setCharType(this.typeFactory.newPrimitiveType(JClassNamespace.of(char.class), JClassNamespace.of(Character.class), JDescriptor.Primitive.CHAR))
                .setIntType(this.typeFactory.newPrimitiveType(JClassNamespace.of(int.class), JClassNamespace.of(Integer.class), JDescriptor.Primitive.INT))
                .setLongType(this.typeFactory.newPrimitiveType(JClassNamespace.of(long.class), JClassNamespace.of(Long.class), JDescriptor.Primitive.LONG))
                .setFloatType(this.typeFactory.newPrimitiveType(JClassNamespace.of(float.class), JClassNamespace.of(Float.class), JDescriptor.Primitive.FLOAT))
                .setDoubleType(this.typeFactory.newPrimitiveType(JClassNamespace.of(double.class), JClassNamespace.of(Double.class), JDescriptor.Primitive.DOUBLE))
                .initPrimitiveMaps();

        constants.setObject(this.tryLocResolve(Object.class))
                .setCloneable(this.tryLocResolve(Cloneable.class))
                .setSerializable(this.tryLocResolve(Serializable.class))
                .setRuntimeException(this.tryLocResolve(RuntimeException.class));

        this.operations = new JTypeOperationsImpl(this);
    }

    private JClassReference tryLocResolve(Class<?> cls) {
        return (JClassReference) this.resolve(JClassLocation.class, JType.class, JClassLocation.of(cls))
                .getOrThrow();
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
    public String toString() {
        return "JSimpleTypeSystem[" + this.name + "]";
    }

    @Override
    public String simpleName() {
        return "Type System: " + this.name;
    }
}
