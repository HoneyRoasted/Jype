package honeyroasted.jype.system;

import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.location.JTypeParameterLocation;
import honeyroasted.jype.system.cache.JTypeStorage;
import honeyroasted.jype.system.expression.JExpressionInspector;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.system.resolver.JTypeResolvers;
import honeyroasted.jype.system.resolver.reflection.JTypeToken;
import honeyroasted.jype.system.solver.operations.JTypeOperations;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.lang.reflect.Executable;
import java.util.Collection;
import java.util.Optional;

public interface JTypeSystem {
    JTypeSystem RUNTIME_REFLECTION = new JSimpleTypeSystem();

    JTypeConstants constants();

    JTypeStorage storage();

    JTypeResolvers resolvers();

    JTypeOperations operations();

    JTypeFactory typeFactory();

    JExpressionInspector expressionInspector();

    <I, O extends JType> Optional<? extends O> resolve(Class<I> keyType, Class<O> resultType, I key);

    <T extends JType> Optional<T> resolve(JTypeToken<?> token);

    <T extends JType> T tryResolve(JTypeToken<?> token);

    <T extends JType> Optional<T> resolve(java.lang.reflect.Type reflectionType);

    <T extends JType> T tryResolve(java.lang.reflect.Type reflectionType);

    <T extends JType> Optional<T> resolve(JClassLocation classLocation);

    <T extends JType> T tryResolve(JClassLocation classLocation);

    <T extends JMethodReference> Optional<T> resolve(Executable executable);

    <T extends JMethodReference> T tryResolve(Executable executable);

    <T extends JMethodReference> Optional<T> resolve(JMethodLocation methodLocation);

    <T extends JMethodReference> T tryResolve(JMethodLocation methodLocation);

    <T extends JVarType> Optional<T> resolve(JTypeParameterLocation parameterLocation);

    <T extends JVarType> T tryResolve(JTypeParameterLocation parameterLocation);

    void registerResolvers(JTypeResolver... resolvers);

    void registerResolvers(Collection<? extends JTypeResolver> resolvers);

    void registerResolver(JTypeResolver resolver);
}
