package honeyroasted.jype.system.operations.result;

import honeyroasted.jype.Namespace;
import honeyroasted.jype.system.operations.TypeOperation;

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class TypeResult<T> {
    private Supplier<T> value;
    private List<Supplier<TypeResult<?>>> causes;
    private BooleanSupplier success;
    private TypeOperation<T> operation;

    public TypeResult(Supplier<T> value, List<Supplier<TypeResult<?>>> causes, BooleanSupplier success, TypeOperation<T> operation) {
        this.value = value;
        this.causes = List.copyOf(causes);
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
        return new TypeResult<>(() -> value, Collections.emptyList(), () -> true, operation);
    }

    public static <T> TypeResult<T> failure(TypeOperation<T> operation) {
        return new TypeResult<>(() -> null, Collections.emptyList(), () -> false, operation);
    }

    public boolean successful() {
        return this.success.getAsBoolean();
    }

    public T value() {
        if (this.successful()) {
            return this.value.get();
        } else {
            return null;
        }
    }

    public List<? extends TypeResult<?>> causes() {
        return this.causes.stream().map(Supplier::get).toList();
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
