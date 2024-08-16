package honeyroasted.jype.system;

import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.system.cache.TypeStorage;
import honeyroasted.jype.system.expression.ExpressionInspector;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.system.resolver.TypeResolvers;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.system.solver.operations.TypeOperations;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.lang.reflect.Executable;
import java.util.Collection;
import java.util.Optional;

public interface TypeSystem {
    TypeSystem SIMPLE_RUNTIME = new SimpleTypeSystem();

    TypeConstants constants();

    TypeStorage storage();

    TypeResolvers resolvers();

    TypeOperations operations();

    TypeFactory typeFactory();

    ExpressionInspector expressionInspector();

    <I, O extends Type> Optional<? extends O> resolve(Class<I> keyType, Class<O> resultType, I key);

    <T extends Type> Optional<T> resolve(TypeToken<?> token);

    <T extends Type> T tryResolve(TypeToken<?> token);

    <T extends Type> Optional<T> resolve(java.lang.reflect.Type reflectionType);

    <T extends Type> T tryResolve(java.lang.reflect.Type reflectionType);

    <T extends Type> Optional<T> resolve(ClassLocation classLocation);

    <T extends Type> T tryResolve(ClassLocation classLocation);

    <T extends MethodReference> Optional<T> resolve(Executable executable);

    <T extends MethodReference> T tryResolve(Executable executable);

    <T extends MethodReference> Optional<T> resolve(MethodLocation methodLocation);

    <T extends MethodReference> T tryResolve(MethodLocation methodLocation);

    <T extends VarType> Optional<T> resolve(TypeParameterLocation parameterLocation);

    <T extends VarType> T tryResolve(TypeParameterLocation parameterLocation);

    void registerResolvers(TypeResolver... resolvers);

    void registerResolvers(Collection<? extends TypeResolver> resolvers);

    void registerResolver(TypeResolver resolver);
}
