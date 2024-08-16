package honeyroasted.jype.system.expression;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.reflection.ReflectionTypeResolution;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.MethodReference;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ReflectionExpressionInspector implements ExpressionInspector {
    private TypeSystem system;

    public ReflectionExpressionInspector(TypeSystem system) {
        this.system = system;
    }

    @Override
    public Optional<Map<MethodLocation, MethodReference>> getDeclaredMethods(ClassReference reference) {
        Optional<Class<?>> clsOpt = ReflectionTypeResolution.getReflectionType(reference);
        if (clsOpt.isEmpty()) return Optional.empty();

        Map<MethodLocation, MethodReference> methods = new LinkedHashMap<>();
        Class<?> cls = clsOpt.get();

        for (Method method : cls.getDeclaredMethods()) {
            MethodLocation loc = MethodLocation.of(method);
            system.resolve(method).ifPresent(mr -> methods.put(loc, mr));
        }

        return Optional.of(methods);
    }

    @Override
    public Optional<Map<MethodLocation, MethodReference>> getDeclaredConstructors(ClassReference reference) {
        Optional<Class<?>> clsOpt = ReflectionTypeResolution.getReflectionType(reference);
        if (clsOpt.isEmpty()) return Optional.empty();

        Map<MethodLocation, MethodReference> methods = new LinkedHashMap<>();
        Class<?> cls = clsOpt.get();

        for (Constructor<?> constructor : cls.getConstructors()) {
            MethodLocation loc = MethodLocation.of(constructor);
            system.resolve(constructor).ifPresent(mr -> methods.put(loc, mr));
        }

        return Optional.of(methods);
    }

    @Override
    public Optional<Boolean> isFunctionalInterface(ClassReference reference) {
        Optional<Class<?>> clsOpt = ReflectionTypeResolution.getReflectionType(reference);
        if (clsOpt.isEmpty()) return Optional.empty();

        Class<?> cls = clsOpt.get();
        if (cls.isInterface()) {
            if (cls.isAnnotationPresent(FunctionalInterface.class)) {
                return Optional.of(true);
            } else {
                boolean found = false;
                for (Method method : cls.getMethods()) {
                    if (!method.isSynthetic() && !method.isDefault() && Modifier.isAbstract(method.getModifiers())) {
                        if (found) {
                            return Optional.of(false);
                        } else {
                            found = true;
                        }
                    }
                }

                return Optional.of(found);
            }
        }
        return Optional.of(false);
    }

}
