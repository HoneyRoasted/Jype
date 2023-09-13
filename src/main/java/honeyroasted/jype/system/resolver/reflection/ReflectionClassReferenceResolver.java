package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.lang.reflect.Array;
import java.lang.reflect.TypeVariable;
import java.util.Optional;

public class ReflectionClassReferenceResolver implements TypeResolver<ClassLocation, ClassReference> {

    @Override
    public Optional<ClassReference> resolve(TypeSystem system, ClassLocation value) {
        try {
            Optional<? extends Type> type = system.resolve(classFromLocation(value));
            if (type.isPresent() && type.get() instanceof ClassReference ref) {
                return Optional.of(ref);
            } else {
                return Optional.empty();
            }
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
