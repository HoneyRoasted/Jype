package honeyroasted.jype.system.solver.solvers.inference.expression;

import honeyroasted.jype.system.resolver.reflection.ReflectionTypeResolution;
import honeyroasted.jype.type.ClassReference;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

public class ReflectionExpressionResolver implements ExpressionResolver {

    public boolean isFunctionalInterface(ClassReference ref) {
        Optional<Class<?>> clsOpt = ReflectionTypeResolution.getReflectionType(ref);
        if (clsOpt.isEmpty()) return false;

        Class<?> cls = clsOpt.get();
        if (cls.isInterface()) {
            if (cls.isAnnotationPresent(FunctionalInterface.class)) {
                return true;
            } else {
                boolean found = false;
                for (Method method : cls.getMethods()) {
                    if (!method.isSynthetic() && !method.isDefault() && Modifier.isAbstract(method.getModifiers())) {
                        if (found) {
                            return false;
                        } else {
                            found = true;
                        }
                    }
                }

                return found;
            }
        }
        return false;
    }

}
