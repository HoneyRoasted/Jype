package honeyroasted.jype.system.expression;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.MethodReference;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MultiExpressionInspector implements ExpressionInspector {
    private List<ExpressionInspector> delegates;

    public MultiExpressionInspector(List<ExpressionInspector> delegates) {
        this.delegates = delegates;
    }

    @Override
    public Optional<Map<MethodLocation, MethodReference>> getAllMethods(ClassReference reference) {
        for (ExpressionInspector inspector : this.delegates) {
            Optional<Map<MethodLocation, MethodReference>> result = inspector.getAllMethods(reference);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<Map<MethodLocation, MethodReference>> getAllConstructors(ClassReference reference) {
        for (ExpressionInspector inspector : this.delegates) {
            Optional<Map<MethodLocation, MethodReference>> result = inspector.getAllMethods(reference);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<Boolean> isFunctionalInterface(ClassReference reference) {
        for (ExpressionInspector inspector : this.delegates) {
            Optional<Boolean> result = inspector.isFunctionalInterface(reference);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }
}
