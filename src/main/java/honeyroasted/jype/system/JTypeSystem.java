package honeyroasted.jype.system;

import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.location.JClassSourceName;
import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.location.JTypeParameterLocation;
import honeyroasted.jype.system.cache.JTypeStorage;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.system.resolver.JTypeResolvers;
import honeyroasted.jype.system.resolver.reflection.JTypeToken;
import honeyroasted.jype.system.solver.operations.JTypeOperations;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.lang.reflect.Executable;
import java.lang.reflect.Type;
import java.util.Collection;

public interface JTypeSystem {
    JTypeSystem RUNTIME_REFLECTION = new JSimpleTypeSystem("RUNTIME_REFLECTION");

    JTypeConstants constants();

    JTypeStorage storage();

    JTypeResolvers resolvers();

    JTypeOperations operations();

    JTypeFactory typeFactory();

    default <I, O extends JType> JResolutionResult<I, O> resolve(Class<I> keyType, Class<O> resultType, I key) {
        return resolvers().resolverFor(keyType, resultType).resolve(this, key);
    }

    default <T extends JType> JResolutionResult<JClassSourceName, T> resolve(JClassSourceName sourceName) {
        return (JResolutionResult) this.resolve(JClassSourceName.class, JType.class, sourceName);
    }

    default  <T extends JType> T tryResolve(JClassSourceName sourceName) {
        return this.<T>resolve(sourceName).getOrThrow();
    }
    
    default <T extends JType> JResolutionResult<JTypeToken<?>, T> resolve(JTypeToken<?> token) {
        return (JResolutionResult) this.resolve(JTypeToken.class, JType.class, token);
    }

    
    default <T extends JType> T tryResolve(JTypeToken<?> token) {
        return this.<T>resolve(token).getOrThrow();
    }

    
    default <T extends JType> JResolutionResult<Type, T> resolve(Type reflectionType) {
        return (JResolutionResult) this.resolve(Type.class, JType.class, reflectionType);
    }

    
    default <T extends JType> T tryResolve(Type reflectionType) {
        return this.<T>resolve(reflectionType).getOrThrow();
    }

    
    default <T extends JType> JResolutionResult<JClassLocation, T> resolve(JClassLocation classLocation) {
        return (JResolutionResult) this.resolve(JClassLocation.class, JType.class, classLocation);
    }

    
    default <T extends JType> T tryResolve(JClassLocation classLocation) {
        return this.<T>resolve(classLocation).getOrThrow();
    }

    
    default <T extends JMethodReference> JResolutionResult<Executable, T> resolve(Executable executable) {
        return (JResolutionResult) this.resolve(Executable.class, JMethodReference.class, executable);
    }

    
    default <T extends JMethodReference> T tryResolve(Executable executable) {
        return this.<T>resolve(executable).getOrThrow();
    }

    
    default <T extends JMethodReference> JResolutionResult<JMethodLocation, T> resolve(JMethodLocation methodLocation) {
        return (JResolutionResult) this.resolve(JMethodLocation.class, JMethodReference.class, methodLocation);
    }

    
    default <T extends JMethodReference> T tryResolve(JMethodLocation methodLocation) {
        return this.<T>resolve(methodLocation).getOrThrow();
    }

    
    default <T extends JVarType> JResolutionResult<JTypeParameterLocation, T> resolve(JTypeParameterLocation parameterLocation) {
        return (JResolutionResult) this.resolve(JTypeParameterLocation.class, JVarType.class, parameterLocation);
    }

    
    default <T extends JVarType> T tryResolve(JTypeParameterLocation parameterLocation) {
        return this.<T>resolve(parameterLocation).getOrThrow();
    }

    void registerResolver(JTypeResolver resolver);

    default void registerResolvers(JTypeResolver... resolvers) {
        for (JTypeResolver resolver : resolvers) {
            this.registerResolver(resolver);
        }
    }

    default void registerResolvers(Collection<? extends JTypeResolver> resolvers) {
        for (JTypeResolver resolver : resolvers) {
            this.registerResolver(resolver);
        }
    }
}
