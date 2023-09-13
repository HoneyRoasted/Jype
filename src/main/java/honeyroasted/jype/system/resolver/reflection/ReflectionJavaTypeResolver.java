package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.type.*;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReflectionJavaTypeResolver implements TypeResolver<java.lang.reflect.Type, Type> {

    @Override
    public Optional<? extends Type> resolve(TypeSystem system, java.lang.reflect.Type value) {
        Optional<Type> cached = system.storage().cacheFor(java.lang.reflect.Type.class).get(value);
        if (cached.isPresent()) {
            return cached;
        }

        if (value instanceof TypeVariable<?> tVar) {
            return system.resolvers().resolverFor(TypeParameterLocation.class, VarType.class).resolve(system, TypeParameterLocation.of(tVar));
        } else if (value instanceof Class<?> cls) {
            if (cls.isPrimitive()) {
                if (cls.equals(void.class)) {
                    return Optional.ofNullable(system.constants().voidType());
                } else {
                    return system.constants().allPrimitives().stream().filter(t -> t.namespace().location().equals(ClassLocation.of(cls))).findFirst();
                }
            } else {
                ClassReference reference = new ClassReference(system);
                system.storage().cacheFor(java.lang.reflect.Type.class).put(value, reference);

                reference.setNamespace(ClassNamespace.of(cls));

                if (cls.getSuperclass() != null) {
                    Optional<? extends ClassReference> superCls = system.resolvers().resolverFor(ClassLocation.class, ClassReference.class)
                            .resolve(system, ClassLocation.of(cls.getSuperclass()));

                    if (superCls.isEmpty()) {
                        system.storage().cacheFor(ClassLocation.class).remove(value);
                        return Optional.empty();
                    } else {
                        reference.setSuperClass(superCls.get());
                    }
                }

                for (Class<?> inter : cls.getInterfaces()) {
                    Optional<? extends ClassReference> interRef = system.resolvers().resolverFor(ClassLocation.class, ClassReference.class)
                            .resolve(system, ClassLocation.of(inter));

                    if (interRef.isEmpty()) {
                        system.storage().cacheFor(ClassLocation.class).remove(value);
                        return Optional.empty();
                    } else {
                        reference.interfaces().add(interRef.get());
                    }
                }

                for (TypeVariable<?> param : cls.getTypeParameters()) {
                    TypeParameterLocation loc = new TypeParameterLocation(reference.namespace().location(), param.getName());
                    Optional<? extends VarType> paramRef = system.resolvers().resolverFor(TypeParameterLocation.class, VarType.class)
                            .resolve(system, loc);

                    if (paramRef.isEmpty()) {
                        system.storage().cacheFor(ClassLocation.class).remove(value);
                        return Optional.empty();
                    } else {
                        reference.typeParameters().add(paramRef.get());
                    }
                }

                reference.setUnmodifiable(true);
                return Optional.of(reference);
            }
        } else if (value instanceof GenericArrayType genArrType) {
            return system.resolvers().resolverFor(java.lang.reflect.Type.class, Type.class).resolve(system, genArrType.getGenericComponentType())
                    .map(t -> new ArrayType(system, t));
        } else if (value instanceof WildcardType wType) {
            java.lang.reflect.Type[] bounds = wType.getLowerBounds().length == 0 ? wType.getUpperBounds() : wType.getLowerBounds();
            List<Type> resolvedBounds = new ArrayList<>();

            for (java.lang.reflect.Type bound : bounds) {
                Optional<? extends Type> resolved = system.resolvers().resolverFor(java.lang.reflect.Type.class, Type.class).resolve(system, bound);
                if (resolved.isPresent()) {
                    resolvedBounds.add(resolved.get());
                } else {
                    return Optional.empty();
                }
            }

            if (wType.getLowerBounds().length == 0) { //? extends ...
                return Optional.of(new WildType.Upper(system, resolvedBounds));
            } else { //? super ...
                return Optional.of(new WildType.Lower(system, resolvedBounds));
            }
        } else if (value instanceof ParameterizedType pType) {
            if (pType.getRawType() instanceof Class<?> cls) {
                Optional<? extends ClassReference> clsRef = system.resolvers().resolverFor(ClassLocation.class, ClassReference.class).resolve(system, ClassLocation.of(cls));
                if (clsRef.isPresent()) {
                    ClassType result = new ClassType(system);
                    system.storage().cacheFor(java.lang.reflect.Type.class).put(value, result);
                    result.setClassReference(clsRef.get());

                    List<Type> typeArguments = new ArrayList<>();
                    for(java.lang.reflect.Type arg : pType.getActualTypeArguments()) {
                        Optional<? extends Type> argResolved = system.resolvers().resolverFor(java.lang.reflect.Type.class, Type.class).resolve(system, arg);
                        if (argResolved.isPresent()) {
                            typeArguments.add(argResolved.get());
                        } else {
                            system.storage().cacheFor(java.lang.reflect.Type.class).remove(value);
                            return Optional.empty();
                        }
                    }

                    result.setTypeArguments(typeArguments);
                    result.setUnmodifiable(true);

                    return Optional.of(result);
                }
            }
        }

        return Optional.empty();
    }

}
