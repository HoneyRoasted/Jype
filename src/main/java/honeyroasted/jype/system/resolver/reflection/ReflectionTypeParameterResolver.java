package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.location.ClassLocation;
import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.location.TypeParameterLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.TypeResolver;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.Optional;

public class ReflectionTypeParameterResolver implements TypeResolver<TypeParameterLocation, VarType> {

    @Override
    public Optional<VarType> resolve(TypeSystem system, TypeParameterLocation value) {
        Optional<VarType> cached = system.storage().<TypeParameterLocation, VarType>cacheFor(TypeParameterLocation.class).get(value);
        if (cached.isPresent()) {
            return cached;
        }

        try {
            Class<?> target = ReflectionClassReferenceResolver.classFromLocation(value.containing().containingClass());

            TypeVariable[] parameters = null;
            if (value.containing() instanceof ClassLocation) {
                parameters = target.getTypeParameters();
            } else if (value.containing() instanceof MethodLocation mloc) {
                boolean found = false;
                if (mloc.name().equals(MethodLocation.CONSTRUCTOR_NAME)) {
                    Constructor[] constructors = target.getDeclaredConstructors();
                    for (Constructor cons : constructors) {
                        if (matches(mloc, cons)) {
                            found = true;
                            parameters = cons.getTypeParameters();
                            break;
                        }
                    }
                } else {
                    Method[] methods = target.getDeclaredMethods();
                    for (Method method : methods) {
                        if (matches(mloc, method)) {
                            found = true;
                            parameters = method.getTypeParameters();
                            break;
                        }
                    }
                }

                if (!found) {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }

            TypeVariable varTarget = null;
            for (TypeVariable var : parameters) {
                if (var.getName().equals(value.name())) {
                    varTarget = var;
                    break;
                }
            }

            if (varTarget != null) {
                Optional<? extends Type> var = system.resolve(java.lang.reflect.Type.class, Type.class, varTarget);
                if (var.isPresent() && var.get() instanceof VarType vType) {
                    return Optional.of(vType);
                } else {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    public static boolean matches(MethodLocation methodLocation, Executable method) {
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

}
