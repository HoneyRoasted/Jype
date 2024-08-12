package honeyroasted.jype.system.expression;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.MethodReference;

import java.util.Map;
import java.util.Optional;

public interface ExpressionInspector {

    Optional<Map<MethodLocation, MethodReference>> getDeclaredMethods(TypeSystem system, ClassReference reference);

    Optional<Boolean> isFunctionalInterface(TypeSystem system, ClassReference reference);

}
