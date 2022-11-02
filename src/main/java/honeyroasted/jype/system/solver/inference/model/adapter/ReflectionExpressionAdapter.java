package honeyroasted.jype.system.solver.inference.model.adapter;

import honeyroasted.jype.type.TypeDeclaration;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ReflectionExpressionAdapter implements ExpressionAdapter {

    @Override
    public boolean isFunctionalInterface(TypeDeclaration type) {
        try {
            Class<?> cls = Class.forName(type.name());
            if (cls.isAnnotationPresent(FunctionalInterface.class)) {
                return true;
            } else {
                int abstractMethods = 0;
                for (Method method : cls.getMethods()) {
                    if (!method.getDeclaringClass().equals(Object.class) &&
                            Modifier.isAbstract(method.getModifiers())) {
                        abstractMethods++;
                    }
                }
                return abstractMethods == 1;
            }
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
