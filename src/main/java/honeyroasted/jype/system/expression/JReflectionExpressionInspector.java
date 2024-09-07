package honeyroasted.jype.system.expression;

import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.resolver.reflection.JReflectionTypeResolution;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JMethodReference;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class JReflectionExpressionInspector implements JExpressionInspector {
    private JTypeSystem system;

    public JReflectionExpressionInspector(JTypeSystem system) {
        this.system = system;
    }

    @Override
    public Optional<Map<JMethodLocation, JMethodReference>> getAllMethods(JClassReference reference) {
        Optional<Class<?>> clsOpt = JReflectionTypeResolution.getReflectionType(reference);
        if (clsOpt.isEmpty()) return Optional.empty();

        Map<JMethodLocation, JMethodReference> methods = new LinkedHashMap<>();
        Class<?> cls = clsOpt.get();

        for (Method method : JReflectionTypeResolution.getAllMethods(cls)) {
            JMethodLocation loc = JMethodLocation.of(method);
            system.resolve(method).ifPresent(mr -> methods.put(loc, mr));
        }

        return Optional.of(methods);
    }

    @Override
    public Optional<Map<JMethodLocation, JMethodReference>> getAllConstructors(JClassReference reference) {
        Optional<Class<?>> clsOpt = JReflectionTypeResolution.getReflectionType(reference);
        if (clsOpt.isEmpty()) return Optional.empty();

        Map<JMethodLocation, JMethodReference> methods = new LinkedHashMap<>();
        Class<?> cls = clsOpt.get();

        for (Constructor<?> constructor : JReflectionTypeResolution.getAllConstructors(cls)) {
            JMethodLocation loc = JMethodLocation.of(constructor);
            system.resolve(constructor).ifPresent(mr -> methods.put(loc, mr));
        }

        return Optional.of(methods);
    }

    @Override
    public Optional<Boolean> isFunctionalInterface(JClassReference reference) {
        Optional<Class<?>> clsOpt = JReflectionTypeResolution.getReflectionType(reference);
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
