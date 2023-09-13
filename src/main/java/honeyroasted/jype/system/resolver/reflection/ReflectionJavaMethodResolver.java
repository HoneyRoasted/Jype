package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReflectionJavaMethodResolver implements TypeResolver<Executable, MethodReference> {

    @Override
    public Optional<? extends MethodReference> resolve(TypeSystem system, Executable value) {
        Optional<Type> cached = system.storage().cacheFor(Executable.class).get(value);
        if (cached.isPresent() && cached.get() instanceof MethodReference mRef) {
            return Optional.of(mRef);
        }

        MethodLocation location;

        if (value instanceof Method method) {
            location = MethodLocation.of(method);
        } else if (value instanceof Constructor<?> constructor) {
            location = MethodLocation.of(constructor);
        } else {
            return Optional.empty();
        }

        Optional<Type> locCached = system.storage().cacheFor(MethodLocation.class).get(location);
        if (locCached.isPresent() && locCached.get() instanceof MethodReference mRef) {
            return Optional.of(mRef);
        }

        MethodReference mRef = new MethodReference(system);
        mRef.setLocation(location);

        if (value instanceof Method method) {
            Optional<? extends Type> returnType = system.resolve(java.lang.reflect.Type.class, Type.class, method.getGenericReturnType());
            if (returnType.isPresent()) {
                mRef.setReturnType(returnType.get());
            } else {
                return Optional.empty();
            }
        } else {
            mRef.setReturnType(system.constants().voidType());
        }

        List<Type> resolvedParams = new ArrayList<>();
        for (java.lang.reflect.Type param : value.getGenericParameterTypes()) {
            Optional<? extends Type> resolved = system.resolve(java.lang.reflect.Type.class, Type.class, param);
            if (resolved.isPresent()) {
                resolvedParams.add(resolved.get());
            } else {
                return Optional.empty();
            }
        }

        List<VarType> resolvedTypeParams = new ArrayList<>();
        for (TypeVariable<?> tvar : value.getTypeParameters()) {
            Optional<? extends Type> resolved = system.resolve(java.lang.reflect.Type.class, Type.class, tvar);
            if (resolved.isPresent() && resolved.get() instanceof VarType res) {
                resolvedTypeParams.add(res);
            } else {
                return Optional.empty();
            }
        }

        mRef.setParameters(resolvedParams);
        mRef.setTypeParameters(resolvedTypeParams);
        mRef.setUnmodifiable(true);
        system.storage().cacheFor(MethodLocation.class).put(mRef.location(), mRef);
        system.storage().cacheFor(Executable.class).put(value, mRef);

        return Optional.of(mRef);
    }

}
