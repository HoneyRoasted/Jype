package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.metadata.JReflectionType;
import honeyroasted.jype.metadata.location.JClassLocation;
import honeyroasted.jype.metadata.location.JTypeParameterLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.system.resolver.JTypeResolver;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JReflectionJavaTypeResolver implements JTypeResolver<Type, JType> {

    @Override
    public JResolutionResult<Type, JType> resolve(JTypeSystem system, Type value) {
        JResolutionResult<Type, JType> cached = system.storage().cacheFor(Type.class).asResolution(value);
        if (cached.success()) {
            return cached;
        }

        if (value instanceof TypeVariable<?> tVar) {
            JTypeParameterLocation location = JTypeParameterLocation.of(tVar);
            JResolutionResult<JTypeParameterLocation, JType> varCached = system.storage().cacheFor(JTypeParameterLocation.class).asResolution(location);
            if (varCached.success()) {
                return JResolutionResult.inherit(value, varCached);
            }

            JResolutionResult<JTypeParameterLocation, JVarType> attemptByLocation = system.resolve(location);
            if (attemptByLocation.success()) {
                return JResolutionResult.inherit(value, attemptByLocation);
            } else {
                return (JResolutionResult) JReflectionTypeResolution.createVarType(system, tVar, location);
            }
        } else if (value instanceof Class<?> cls) {
            if (cls.isPrimitive()) {
                if (cls.equals(void.class)) {
                    return new JResolutionResult<>(system.constants().voidType(), cls);
                } else {
                    return JResolutionResult.inherit(cls, system.constants().allPrimitives().stream().filter(t -> t.namespace().location().equals(JClassLocation.of(cls))).findFirst(),
                            "Unknown primitive type");
                }
            } else {
                JClassLocation location = JClassLocation.of(cls);
                JResolutionResult<JClassLocation, JType> clsCached = system.storage().cacheFor(JClassLocation.class).asResolution(location);
                if (clsCached.success()) {
                    return JResolutionResult.inherit(value, clsCached);
                }

                JResolutionResult<JClassLocation, JType> attemptByLocation = system.resolve(location);
                if (attemptByLocation.success()) {
                    return JResolutionResult.inherit(value, attemptByLocation);
                } else {
                    return (JResolutionResult) JReflectionTypeResolution.createClassReference(system, cls, location);
                }
            }
        } else if (value instanceof GenericArrayType genArrType) {
            return system.resolvers().resolverFor(Type.class, JType.class).resolve(system, genArrType.getGenericComponentType())
                    .map(value, t -> {
                        JArrayType result = t.typeSystem().typeFactory().newArrayType();
                        result.metadata().attach(new JReflectionType.Type(genArrType));
                        result.setComponent(t);
                        result.setUnmodifiable(true);
                        return result;
                    }, "Failed to create array type");
        } else if (value instanceof WildcardType wType) {
            List<JResolutionResult<?, ?>> children = new ArrayList<>();

            Type[] bounds = wType.getLowerBounds().length == 0 ? wType.getUpperBounds() : wType.getLowerBounds();
            Set<JType> resolvedBounds = new LinkedHashSet<>();

            for (Type bound : bounds) {
                JResolutionResult<Type, JType> resolved = system.resolvers().resolverFor(Type.class, JType.class).resolve(system, bound);
                children.add(resolved);
                if (resolved.success()) {
                    resolvedBounds.add(resolved.value());
                } else {
                    return new JResolutionResult<>("Failed to resolve classBound", value, children);
                }
            }

            if (wType.getLowerBounds().length == 0) { //? extends ...
                JWildType.Upper upper = system.typeFactory().newUpperWildType();
                upper.metadata().attach(new JReflectionType.Type(wType));
                upper.setIdentity(System.identityHashCode(wType));
                upper.upperBounds().addAll(resolvedBounds);
                upper.setUnmodifiable(true);
                return new JResolutionResult<>(upper, value, children);
            } else { //? super ...
                JWildType.Lower lower = system.typeFactory().newLowerWildType();
                lower.metadata().attach(new JReflectionType.Type(wType));
                lower.setIdentity(System.identityHashCode(wType));
                lower.lowerBounds().addAll(resolvedBounds);
                lower.setUnmodifiable(true);
                return new JResolutionResult<>(lower, value, children);
            }
        } else if (value instanceof ParameterizedType pType) {
            if (pType.getRawType() instanceof Class<?> cls) {
                List<JResolutionResult<?, ?>> children = new ArrayList<>();

                JResolutionResult<Type, JType> clsRef = system.resolvers().resolverFor(Type.class, JType.class).resolve(system, cls);
                children.add(clsRef);
                if (clsRef.success() && clsRef.value() instanceof JClassReference) {
                    JParameterizedClassType result = system.typeFactory().newParameterizedClassType();
                    result.metadata().attach(new JReflectionType.Type(pType));

                    system.storage().cacheFor(Type.class).put(value, result);
                    result.setClassReference((JClassReference) clsRef.value());

                    List<JArgumentType> typeArguments = new ArrayList<>();
                    for (Type arg : pType.getActualTypeArguments()) {
                        JResolutionResult<Type, JType> argResolved = system.resolvers().resolverFor(Type.class, JType.class).resolve(system, arg);
                        children.add(argResolved);
                        if (argResolved.success() && argResolved.value() instanceof JArgumentType at) {
                            typeArguments.add(at);
                        } else {
                            system.storage().cacheFor(Type.class).remove(value);
                            return new JResolutionResult<>("Failed to resolve type argument", value, children);
                        }
                    }

                    result.setTypeArguments(typeArguments);

                    if (pType.getOwnerType() != null) {
                        JResolutionResult<Type, JType> owner = system.resolvers().resolverFor(Type.class, JType.class).resolve(system, pType.getOwnerType());
                        children.add(owner);
                        if (owner.success() && owner.value() instanceof JClassType ct) {
                            result.setOuterType(ct);
                        } else {
                            system.storage().cacheFor(Type.class).remove(value);
                            return new JResolutionResult<>("Failed to resolve outer class", value, children);
                        }
                    }

                    result.setUnmodifiable(true);
                    return new JResolutionResult<>(result, value, children);
                } else {
                    return new JResolutionResult<>("Failed to resolve class reference", value, children);
                }
            } else {
                return new JResolutionResult<>("Unknown raw type: " + pType.getRawType().getClass().getName(), value);
            }
        } else {
            return new JResolutionResult<>("Unknown reflection type: " + value.getClass().getName(), value);
        }
    }

}
