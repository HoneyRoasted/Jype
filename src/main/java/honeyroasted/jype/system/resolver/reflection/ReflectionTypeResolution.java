package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.system.resolver.BundledTypeResolvers;
import honeyroasted.jype.system.resolver.exception.ResolutionFailedException;

import java.lang.reflect.*;

public interface ReflectionTypeResolution {

    BundledTypeResolvers REFLECTION_TYPE_RESOLVERS = new BundledTypeResolvers(
            new ReflectionTypeTokenResolver(),
            new ReflectionJavaTypeResolver(),
            new ReflectionJavaMethodResolver(),
            new ReflectionClassReferenceResolver(),
            new ReflectionMethodReferenceResolver(),
            new ReflectionTypeParameterResolver()
    );

    static boolean locationMatchesMethod(MethodLocation methodLocation, Executable method) {
        ClassLocation retType = method instanceof Method mth ? ClassLocation.of(mth.getReturnType()) : ClassLocation.of(void.class);

        if (methodLocation.name().equals(method.getName()) &&
                methodLocation.containing().equals(ClassLocation.of(method.getDeclaringClass())) &&
                methodLocation.returnType().equals(retType) &&
                methodLocation.parameters().size() == method.getParameterCount()) {
            Class[] parameters = method.getParameterTypes();

            for (int i = 0; i < parameters.length; i++) {
                if (!methodLocation.parameters().get(i).equals(ClassLocation.of(parameters[i]))) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    static Class<?> classFromLocation(ClassLocation location) throws ResolutionFailedException {
        try {
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
        } catch (ClassNotFoundException ex) {
            throw new ResolutionFailedException("Failed to resolve class: " + location, ex);
        }
    }

    static Executable methodFromLocation(MethodLocation location) throws ResolutionFailedException {
        Class<?> targetCls;
        try {
            targetCls = classFromLocation(location.containing());
        } catch (ResolutionFailedException ex) {
            throw new ResolutionFailedException("Could not resolve method: " + location, ex);
        }
        Executable targetMethod = null;

        if (location.name().equals(MethodLocation.CONSTRUCTOR_NAME)) {
            Constructor[] cons = targetCls.getDeclaredConstructors();
            for (Constructor con : cons) {
                if (locationMatchesMethod(location, con)) {
                    targetMethod = con;
                    break;
                }
            }
        } else {
            Method[] methods = targetCls.getDeclaredMethods();
            for (Method method : methods) {
                if (locationMatchesMethod(location, method)) {
                    targetMethod = method;
                    break;
                }
            }
        }

        if (targetMethod == null) {
            throw new ResolutionFailedException("Could not resolve method: " + location);
        }

        return targetMethod;
    }

    static TypeVariable<?> typeParameterFromLocation(TypeParameterLocation location) throws ResolutionFailedException {
        TypeVariable[] parameters = null;

        try {
            if (location.containing() instanceof ClassLocation containing) {
                parameters = ReflectionTypeResolution.classFromLocation(containing).getTypeParameters();
            } else if (location.containing() instanceof MethodLocation containing) {
                parameters = ReflectionTypeResolution.methodFromLocation(containing).getTypeParameters();
            }
        } catch (ResolutionFailedException ex) {
            throw new ResolutionFailedException("Could not resolve type parameter: " + location, ex);
        }

        if (parameters == null) {
            throw new ResolutionFailedException("Could not resolve type parameter: " + location);
        }

        TypeVariable varTarget = null;
        for (TypeVariable var : parameters) {
            if (var.getName().equals(location.name())) {
                varTarget = var;
                break;
            }
        }

        if (varTarget != null) {
            return varTarget;
        } else {
            throw new ResolutionFailedException("Could not resolve type parameter: " + location);
        }
    }

}
