package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.JClassLocation;
import honeyroasted.jype.location.JTypeParameterLocation;
import honeyroasted.jype.system.JTypeSystem;
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
import java.util.Optional;
import java.util.Set;

public class JReflectionJavaTypeResolver implements JTypeResolver<Type, JType> {

    @Override
    public Optional<? extends JType> resolve(JTypeSystem system, Type value) {
        Optional<JType> cached = system.storage().cacheFor(Type.class).get(value);
        if (cached.isPresent()) {
            return cached;
        }

        if (value instanceof TypeVariable<?> tVar) {
            JTypeParameterLocation location = JTypeParameterLocation.of(tVar);
            Optional<JType> varCached = system.storage().cacheFor(JTypeParameterLocation.class).get(location);
            if (varCached.isPresent()) {
                return varCached;
            }

            Optional<? extends JVarType> attemptByLocation = system.resolve(location);
            if (attemptByLocation.isPresent()) {
                return attemptByLocation;
            } else {
                return JReflectionTypeResolution.createVarType(system, tVar, location);
            }
        } else if (value instanceof Class<?> cls) {
            if (cls.isPrimitive()) {
                if (cls.equals(void.class)) {
                    return Optional.ofNullable(system.constants().voidType());
                } else {
                    return system.constants().allPrimitives().stream().filter(t -> t.namespace().location().equals(JClassLocation.of(cls))).findFirst();
                }
            } else {
                JClassLocation location = JClassLocation.of(cls);
                Optional<JType> clsCached = system.storage().cacheFor(JClassLocation.class).get(location);
                if (clsCached.isPresent()) {
                    return clsCached;
                }

                Optional<? extends JType> attemptByLocation = system.resolve(location);
                if (attemptByLocation.isPresent()) {
                    return attemptByLocation;
                } else {
                    return JReflectionTypeResolution.createClassReference(system, cls, location);
                }
            }
        } else if (value instanceof GenericArrayType genArrType) {
            return system.resolvers().resolverFor(Type.class, JType.class).resolve(system, genArrType.getGenericComponentType())
                    .map(t -> {
                        JArrayType result = t.typeSystem().typeFactory().newArrayType();
                        result.metadata().attach(new JReflectionType.Type(genArrType));
                        result.setComponent(t);
                        result.setUnmodifiable(true);
                        return result;
                    });
        } else if (value instanceof WildcardType wType) {
            Type[] bounds = wType.getLowerBounds().length == 0 ? wType.getUpperBounds() : wType.getLowerBounds();
            Set<JType> resolvedBounds = new LinkedHashSet<>();

            for (Type bound : bounds) {
                Optional<? extends JType> resolved = system.resolvers().resolverFor(Type.class, JType.class).resolve(system, bound);
                if (resolved.isPresent()) {
                    resolvedBounds.add(resolved.get());
                } else {
                    return Optional.empty();
                }
            }

            if (wType.getLowerBounds().length == 0) { //? extends ...
                JWildType.Upper upper = system.typeFactory().newUpperWildType();
                upper.metadata().attach(new JReflectionType.Type(wType));
                upper.setIdentity(System.identityHashCode(wType));
                upper.upperBounds().addAll(resolvedBounds);
                upper.setUnmodifiable(true);
                return Optional.of(upper);
            } else { //? super ...
                JWildType.Lower lower = system.typeFactory().newLowerWildType();
                lower.metadata().attach(new JReflectionType.Type(wType));
                lower.setIdentity(System.identityHashCode(wType));
                lower.lowerBounds().addAll(resolvedBounds);
                lower.setUnmodifiable(true);
                return Optional.of(lower);
            }
        } else if (value instanceof ParameterizedType pType) {
            if (pType.getRawType() instanceof Class<?> cls) {
                Optional<? extends JType> clsRef = system.resolvers().resolverFor(Type.class, JType.class).resolve(system, cls);
                if (clsRef.isPresent() && clsRef.get() instanceof JClassReference) {
                    JParameterizedClassType result = system.typeFactory().newParameterizedClassType();
                    result.metadata().attach(new JReflectionType.Type(pType));

                    system.storage().cacheFor(Type.class).put(value, result);
                    result.setClassReference((JClassReference) clsRef.get());

                    List<JArgumentType> typeArguments = new ArrayList<>();
                    for (Type arg : pType.getActualTypeArguments()) {
                        Optional<? extends JType> argResolved = system.resolvers().resolverFor(Type.class, JType.class).resolve(system, arg);
                        if (argResolved.isPresent() && argResolved.get() instanceof JArgumentType at) {
                            typeArguments.add(at);
                        } else {
                            system.storage().cacheFor(Type.class).remove(value);
                            return Optional.empty();
                        }
                    }

                    result.setTypeArguments(typeArguments);

                    if (pType.getOwnerType() != null) {
                        Optional<? extends JType> owner = system.resolvers().resolverFor(Type.class, JType.class).resolve(system, pType.getOwnerType());
                        if (owner.isPresent() && owner.get() instanceof JClassType ct) {
                            result.setOuterType(ct);
                        } else {
                            system.storage().cacheFor(Type.class).remove(value);
                            return Optional.empty();
                        }
                    }

                    result.setUnmodifiable(true);
                    return Optional.of(result);
                }
            }
        }

        return Optional.empty();
    }

}
