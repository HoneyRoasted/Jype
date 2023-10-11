package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;
import honeyroasted.jype.type.impl.ArrayTypeImpl;
import honeyroasted.jype.type.impl.ParameterizedClassTypeImpl;
import honeyroasted.jype.type.impl.WildTypeLowerImpl;
import honeyroasted.jype.type.impl.WildTypeUpperImpl;
import honeyroasted.jype.type.meta.ArrayTypeMeta;
import honeyroasted.jype.type.meta.ParameterizedClassTypeMeta;
import honeyroasted.jype.type.meta.WildTypeLowerMeta;
import honeyroasted.jype.type.meta.WildTypeUpperMeta;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ReflectionJavaTypeResolver implements TypeResolver<java.lang.reflect.Type, Type> {

    @Override
    public Optional<? extends Type> resolve(TypeSystem system, java.lang.reflect.Type value) {
        Optional<Type> cached = system.storage().cacheFor(java.lang.reflect.Type.class).get(value);
        if (cached.isPresent()) {
            return cached;
        }

        if (value instanceof TypeVariable<?> tVar) {
            TypeParameterLocation location = TypeParameterLocation.of(tVar);
            Optional<Type> varCached = system.storage().cacheFor(TypeParameterLocation.class).get(location);
            if (varCached.isPresent()) {
                return varCached;
            }

            Optional<? extends VarType> attemptByLocation = system.resolve(location);
            if (attemptByLocation.isPresent()) {
                return attemptByLocation;
            } else {
                return ReflectionTypeResolution.createVarType(system, tVar, location);
            }
        } else if (value instanceof Class<?> cls) {
            if (cls.isPrimitive()) {
                if (cls.equals(void.class)) {
                    return Optional.ofNullable(system.constants().voidType());
                } else {
                    return system.constants().allPrimitives().stream().filter(t -> t.namespace().location().equals(ClassLocation.of(cls))).findFirst();
                }
            } else {
                ClassLocation location = ClassLocation.of(cls);
                Optional<Type> clsCached = system.storage().cacheFor(ClassLocation.class).get(location);
                if (clsCached.isPresent()) {
                    return clsCached;
                }

                Optional<? extends ClassReference> attemptByLocation = system.resolve(location);
                if (attemptByLocation.isPresent()) {
                    return attemptByLocation;
                } else {
                    return ReflectionTypeResolution.createClassReference(system, cls, location);
                }
            }
        } else if (value instanceof GenericArrayType genArrType) {
            return system.resolvers().resolverFor(java.lang.reflect.Type.class, Type.class).resolve(system, genArrType.getGenericComponentType())
                    .map(t -> {
                        ArrayType result = new ArrayTypeImpl(t.typeSystem());
                        ArrayTypeMeta<GenericArrayType> attached = new ArrayTypeMeta<>(system, k -> result);
                        attached.setMetadata(genArrType);

                        result.setComponent(t);
                        result.setUnmodifiable(true);
                        return attached;
                    });
        } else if (value instanceof WildcardType wType) {
            java.lang.reflect.Type[] bounds = wType.getLowerBounds().length == 0 ? wType.getUpperBounds() : wType.getLowerBounds();
            Set<Type> resolvedBounds = new LinkedHashSet<>();

            for (java.lang.reflect.Type bound : bounds) {
                Optional<? extends Type> resolved = system.resolvers().resolverFor(java.lang.reflect.Type.class, Type.class).resolve(system, bound);
                if (resolved.isPresent()) {
                    resolvedBounds.add(resolved.get());
                } else {
                    return Optional.empty();
                }
            }

            if (wType.getLowerBounds().length == 0) { //? extends ...
                WildType.Upper upper = new WildTypeUpperImpl(system);
                WildTypeUpperMeta<WildcardType> attached = new WildTypeUpperMeta<>(system, t -> upper);
                attached.setMetadata(wType);

                upper.setIdentity(System.identityHashCode(wType));
                upper.upperBounds().addAll(resolvedBounds);
                upper.setUnmodifiable(true);
                return Optional.of(attached);
            } else { //? super ...
                WildType.Lower lower = new WildTypeLowerImpl(system);
                WildTypeLowerMeta<WildcardType> attached = new WildTypeLowerMeta<>(system, t -> lower);
                attached.setMetadata(wType);

                lower.setIdentity(System.identityHashCode(wType));
                lower.lowerBounds().addAll(resolvedBounds);
                lower.setUnmodifiable(true);
                return Optional.of(attached);
            }
        } else if (value instanceof ParameterizedType pType) {
            if (pType.getRawType() instanceof Class<?> cls) {
                Optional<? extends Type> clsRef = system.resolvers().resolverFor(java.lang.reflect.Type.class, Type.class).resolve(system, cls);
                if (clsRef.isPresent() && clsRef.get() instanceof ClassReference) {
                    ParameterizedClassType result = new ParameterizedClassTypeImpl(system);
                    ParameterizedClassTypeMeta<ParameterizedType> attached = new ParameterizedClassTypeMeta<>(system, t -> result);

                    system.storage().cacheFor(java.lang.reflect.Type.class).put(value, attached);
                    result.setClassReference((ClassReference) clsRef.get());

                    List<ArgumentType> typeArguments = new ArrayList<>();
                    for (java.lang.reflect.Type arg : pType.getActualTypeArguments()) {
                        Optional<? extends Type> argResolved = system.resolvers().resolverFor(java.lang.reflect.Type.class, Type.class).resolve(system, arg);
                        if (argResolved.isPresent() && argResolved.get() instanceof ArgumentType at) {
                            typeArguments.add(at);
                        } else {
                            system.storage().cacheFor(java.lang.reflect.Type.class).remove(value);
                            return Optional.empty();
                        }
                    }

                    result.setTypeArguments(typeArguments);

                    if (pType.getOwnerType() != null) {
                        Optional<? extends Type> owner = system.resolvers().resolverFor(java.lang.reflect.Type.class, Type.class).resolve(system, pType.getOwnerType());
                        if (owner.isPresent() && owner.get() instanceof ClassType ct) {
                            result.setOuterType(ct);
                        } else {
                            system.storage().cacheFor(java.lang.reflect.Type.class).remove(value);
                            return Optional.empty();
                        }
                    }

                    result.setUnmodifiable(true);
                    return Optional.of(attached);
                }
            }
        }

        return Optional.empty();
    }

}
