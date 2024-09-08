package honeyroasted.jype.system.expression;

import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JMethodReference;

import java.util.Map;
import java.util.Optional;

public interface JExpressionInspector {

    Optional<Map<JMethodLocation, JMethodReference>> getDeclaredMethods(JClassReference reference);

    Optional<Map<JMethodLocation, JMethodReference>> getDeclaredConstructors(JClassReference reference);

    Optional<Boolean> isFunctionalInterface(JClassReference reference);

}
