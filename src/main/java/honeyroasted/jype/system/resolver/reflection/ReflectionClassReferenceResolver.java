package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.type.VarType;

import java.lang.reflect.Array;
import java.lang.reflect.TypeVariable;
import java.util.Optional;

public class ReflectionClassReferenceResolver implements TypeResolver<ClassLocation, ClassReference> {

    @Override
    public Optional<ClassReference> resolve(TypeSystem system, ClassLocation value) {
        Optional<ClassReference> cached = system.storage().<ClassLocation, ClassReference>cacheFor(ClassLocation.class).get(value);
        if (cached.isPresent()) {
            return cached;
        }

        try {
            Class<?> target = classFromLocation(value);

            ClassReference reference = new ClassReference(system);
            system.storage().cacheFor(ClassLocation.class).put(value, reference);

            reference.setNamespace(ClassNamespace.of(target));

            if (target.getSuperclass() != null) {
                Optional<? extends ClassReference> superCls = system.resolvers().resolverFor(ClassLocation.class, ClassReference.class)
                        .resolve(system, ClassLocation.of(target.getSuperclass()));

                if (superCls.isEmpty()) {
                    system.storage().cacheFor(ClassLocation.class).remove(value);
                    return Optional.empty();
                } else {
                    reference.setSuperClass(superCls.get());
                }
            }

            for (Class<?> inter : target.getInterfaces()) {
                Optional<? extends ClassReference> interRef = system.resolvers().resolverFor(ClassLocation.class, ClassReference.class)
                        .resolve(system, ClassLocation.of(inter));

                if (interRef.isEmpty()) {
                    system.storage().cacheFor(ClassLocation.class).remove(value);
                    return Optional.empty();
                } else {
                    reference.interfaces().add(interRef.get());
                }
            }

            for (TypeVariable<?> param : target.getTypeParameters()) {
                TypeParameterLocation loc = new TypeParameterLocation(value, param.getName());
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
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    public static Class<?> classFromLocation(ClassLocation location) throws ClassNotFoundException {
        int depth = 0;
        ClassLocation current = location;
        while (current.isArray()) {
            current = current.containing();
            depth++;
        }

        Class<?> target = Class.forName(current.toRuntimeName());

        for (int i = 0; i < depth; i++) {
            target = Array.newInstance(target, 0).getClass();
        }

        return target;
    }

}
