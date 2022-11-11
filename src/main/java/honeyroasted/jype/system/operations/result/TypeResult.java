package honeyroasted.jype.system.operations.result;

import honeyroasted.jype.Namespace;
import honeyroasted.jype.system.operations.TypeOperation;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class TypeResult<T> {
    private Function<TypeResult<T>, T> value;
    private List<Function<TypeResult<T>, TypeResult<?>>> causes;
    private Predicate<TypeResult<T>> success;
    private TypeOperation<T> operation;

    public TypeResult(Function<TypeResult<T>, T> value, List<Function<TypeResult<T>, TypeResult<?>>> causes, Predicate<TypeResult<T>> success, TypeOperation<T> operation) {
        this.value = value;
        this.causes = causes;
        this.success = success;
        this.operation = operation;
    }

    public static <T> TypeResultBuilder<T> builder(TypeOperation<T> operation) {
        return new TypeResultBuilder<T>().operation(operation);
    }

    public static <T> TypeResult<T> of(TypeOperation<T> operation) {
        return operation.perform();
    }

    public static <T> TypeResult<T> success(T value, TypeOperation<T> operation) {
        return new TypeResult<>(t -> value, Collections.emptyList(), t -> true, operation);
    }

    public static <T> TypeResult<T> failure(TypeOperation<T> operation) {
        return new TypeResult<>(t -> null, Collections.emptyList(), t -> false, operation);
    }

    public boolean successful() {
        return this.success.test(this);
    }

    public T value() {
        if (this.successful()) {
            return this.value.apply(this);
        } else {
            return null;
        }
    }

    public List<? extends TypeResult<?>> causes() {
        return this.causes.stream().map(f -> f.apply(this)).toList();
    }

    public TypeOperation<T> operation() {
        return this.operation;
    }

    public String buildMessage() {
        StringBuilder sb = new StringBuilder();
        buildMessage(sb, 0);
        return sb.toString();
    }

    private void buildMessage(StringBuilder sb, int indent) {
        String ind = "|    ".repeat(indent);
        String ls = System.lineSeparator();
        boolean successful = this.successful();

        if (indent > 0) {
            sb.append(ind).append(ls);
        }

        sb.append(ind).append("STATUS: ").append(successful ? "SUCCESS" : "FAILURE").append(ls);

        if (successful) {
            sb.append(ind).append("VALUE: ").append(this.value()).append(ls);
        }

        sb.append(ind).append("OPERATION: ").append(Namespace.of(this.operation.getClass()).simpleName().replace('$', '.')).append(ls);
        sb.append(ind).append("MESSAGE: ").append(this.operation.message()).append(ls);

        if (!this.causes.isEmpty()) {
            sb.append(ind).append("CAUSES: ").append(ls);
            for (TypeResult<?> result : this.causes()) {
                result.buildMessage(sb, indent + 1);
            }
        }
    }

}
