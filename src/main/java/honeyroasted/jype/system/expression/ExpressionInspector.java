package honeyroasted.jype.system.expression;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.MethodReference;

import java.util.Map;
import java.util.Optional;

public interface ExpressionInspector {

    Optional<Map<MethodLocation, MethodReference>> getDeclaredMethods(ClassReference reference);

    Optional<Map<MethodLocation, MethodReference>> getDeclaredConstructors(ClassReference reference);

    Optional<Boolean> isFunctionalInterface(ClassReference reference);

}
