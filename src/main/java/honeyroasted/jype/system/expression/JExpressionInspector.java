package honeyroasted.jype.system.expression;

import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JMethodReference;

import java.util.Map;
import java.util.Optional;

public interface JExpressionInspector {

    Optional<Map<JMethodLocation, JMethodReference>> getAllMethods(JClassReference reference);

    Optional<Map<JMethodLocation, JMethodReference>> getAllConstructors(JClassReference reference);

    Optional<Boolean> isFunctionalInterface(JClassReference reference);

}
