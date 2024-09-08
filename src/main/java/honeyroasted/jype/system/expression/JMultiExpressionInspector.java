package honeyroasted.jype.system.expression;

import honeyroasted.jype.location.JMethodLocation;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JMethodReference;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JMultiExpressionInspector implements JExpressionInspector {
    private List<JExpressionInspector> delegates;

    public JMultiExpressionInspector(List<JExpressionInspector> delegates) {
        this.delegates = delegates;
    }

    @Override
    public Optional<Map<JMethodLocation, JMethodReference>> getDeclaredMethods(JClassReference reference) {
        for (JExpressionInspector inspector : this.delegates) {
            Optional<Map<JMethodLocation, JMethodReference>> result = inspector.getDeclaredMethods(reference);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<Map<JMethodLocation, JMethodReference>> getDeclaredConstructors(JClassReference reference) {
        for (JExpressionInspector inspector : this.delegates) {
            Optional<Map<JMethodLocation, JMethodReference>> result = inspector.getDeclaredConstructors(reference);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<Boolean> isFunctionalInterface(JClassReference reference) {
        for (JExpressionInspector inspector : this.delegates) {
            Optional<Boolean> result = inspector.isFunctionalInterface(reference);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }
}
