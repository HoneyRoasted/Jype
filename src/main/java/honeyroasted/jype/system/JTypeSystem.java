package honeyroasted.jype.system;

import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.location.JClassSourceName;
import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.location.JTypeParameterLocation;
import honeyroasted.jype.system.cache.JTypeStorage;
import honeyroasted.jype.system.resolver.JResolutionAttemptFailedException;
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
import java.util.Optional;

public interface JTypeSystem {
    JTypeSystem RUNTIME_REFLECTION = new JSimpleTypeSystem();

    JTypeConstants constants();

    JTypeStorage storage();

    JTypeResolvers resolvers();

    JTypeOperations operations();

    JTypeFactory typeFactory();

    <I, O extends JType> Optional<? extends O> resolve(Class<I> keyType, Class<O> resultType, I key);

    default <T extends JType> Optional<T> resolve(JClassSourceName sourceName) {
        return (Optional<T>) this.resolve(JClassSourceName.class, JType.class, sourceName);
    }

    default  <T extends JType> T tryResolve(JClassSourceName sourceName) {
        return this.<T>resolve(sourceName).orElseThrow(() -> new JResolutionAttemptFailedException("Could not resolve " + sourceName + " to a type"));
    }
    
    default <T extends JType> Optional<T> resolve(JTypeToken<?> token) {
        return (Optional<T>) this.resolve(JTypeToken.class, JType.class, token);
    }

    
    default <T extends JType> T tryResolve(JTypeToken<?> token) {
        return this.<T>resolve(token).orElseThrow(() -> new JResolutionAttemptFailedException("Could not resolve " + token + " to a type"));
    }

    
    default <T extends JType> Optional<T> resolve(Type reflectionType) {
        return (Optional<T>) this.resolve(Type.class, JType.class, reflectionType);
    }

    
    default <T extends JType> T tryResolve(Type reflectionType) {
        return this.<T>resolve(reflectionType).orElseThrow(() -> new JResolutionAttemptFailedException("Could not resolve " + reflectionType + " to a type"));
    }

    
    default <T extends JType> Optional<T> resolve(JClassLocation classLocation) {
        return (Optional<T>) this.resolve(JClassLocation.class, JType.class, classLocation);
    }

    
    default <T extends JType> T tryResolve(JClassLocation classLocation) {
        return this.<T>resolve(classLocation).orElseThrow(() -> new JResolutionAttemptFailedException("Could not resolve " + classLocation + " to a type"));
    }

    
    default <T extends JMethodReference> Optional<T> resolve(Executable executable) {
        return (Optional<T>) this.resolve(Executable.class, JMethodReference.class, executable);
    }

    
    default <T extends JMethodReference> T tryResolve(Executable executable) {
        return this.<T>resolve(executable).orElseThrow(() -> new JResolutionAttemptFailedException("Could not resolve " + executable + " to a type"));
    }

    
    default <T extends JMethodReference> Optional<T> resolve(JMethodLocation methodLocation) {
        return (Optional<T>) this.resolve(JMethodLocation.class, JMethodReference.class, methodLocation);
    }

    
    default <T extends JMethodReference> T tryResolve(JMethodLocation methodLocation) {
        return this.<T>resolve(methodLocation).orElseThrow(() -> new JResolutionAttemptFailedException("Could not resolve " + methodLocation + " to a type"));
    }

    
    default <T extends JVarType> Optional<T> resolve(JTypeParameterLocation parameterLocation) {
        return (Optional<T>) this.resolve(JTypeParameterLocation.class, JVarType.class, parameterLocation);
    }

    
    default <T extends JVarType> T tryResolve(JTypeParameterLocation parameterLocation) {
        return this.<T>resolve(parameterLocation).orElseThrow(() -> new JResolutionAttemptFailedException("Could not resolve " + parameterLocation + " to a type"));
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
