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
            TypeParameterLocation location = TypeParameterLocation.of(tVar);
            VarType varType = new VarType(system);
            varType.setLocation(location);
            system.storage().cacheFor(TypeParameterLocation.class).put(value, varType);

            for (java.lang.reflect.Type bound : tVar.getBounds()) {
                Optional<? extends Type> param = system.resolvers().resolverFor(java.lang.reflect.Type.class, Type.class).resolve(system, bound);
                if (param.isPresent()) {
                    varType.upperBounds().add(param.get());
                } else {
                    system.storage().cacheFor(TypeParameterLocation.class).remove(value);
                    return Optional.empty();
                }
            }

            varType.setUnmodifiable(true);
            return Optional.of(varType);
        } else if (value instanceof Class<?> cls) {
            if (cls.isPrimitive()) {
                if (cls.equals(void.class)) {
                    return Optional.ofNullable(system.constants().voidType());
                } else {
                    return system.constants().allPrimitives().stream().filter(t -> t.namespace().location().equals(ClassLocation.of(cls))).findFirst();
                }
            } else {
                Optional<Type> clsCached = system.storage().cacheFor(ClassLocation.class).get(ClassLocation.of(cls));
                if (clsCached.isPresent()) {
                    return clsCached;
                }

                ClassReference reference = new ClassReference(system);
                reference.setNamespace(ClassNamespace.of(cls));
                reference.setInterface(cls.isInterface());
                system.storage().cacheFor(java.lang.reflect.Type.class).put(value, reference);
                system.storage().cacheFor(ClassLocation.class).put(reference.namespace().location(), reference);

                if (cls.getSuperclass() != null) {
                    Optional<? extends Type> superCls = system.resolvers().resolverFor(java.lang.reflect.Type.class, Type.class)
                            .resolve(system, cls.getGenericSuperclass());

                    if (superCls.isEmpty() || !(superCls.get() instanceof ClassType)) {
                        system.storage().cacheFor(java.lang.reflect.Type.class).remove(value);
                        system.storage().cacheFor(ClassLocation.class).remove(reference.namespace().location());
                        return Optional.empty();
                    } else {
                        reference.setSuperClass((ClassType) superCls.get());
                    }
                }

                for (java.lang.reflect.Type inter : cls.getGenericInterfaces()) {
                    Optional<? extends Type> interRef = system.resolvers().resolverFor(java.lang.reflect.Type.class, Type.class)
                            .resolve(system, inter);

                    if (interRef.isEmpty() || !(interRef.get() instanceof ClassType)) {
                        system.storage().cacheFor(java.lang.reflect.Type.class).remove(value);
                        system.storage().cacheFor(ClassLocation.class).remove(reference.namespace().location());
                        return Optional.empty();
                    } else {
                        reference.interfaces().add((ClassType) interRef.get());
                    }
                }

                for (TypeVariable<?> param : cls.getTypeParameters()) {
                    TypeParameterLocation loc = new TypeParameterLocation(reference.namespace().location(), param.getName());
                    Optional<? extends VarType> paramRef = system.resolvers().resolverFor(TypeParameterLocation.class, VarType.class)
                            .resolve(system, loc);

                    if (paramRef.isEmpty()) {
                        system.storage().cacheFor(java.lang.reflect.Type.class).remove(value);
                        system.storage().cacheFor(ClassLocation.class).remove(reference.namespace().location());
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
                    ParameterizedClassType result = new ParameterizedClassType(system);
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
