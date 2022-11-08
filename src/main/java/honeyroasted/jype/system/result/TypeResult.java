package honeyroasted.jype.system.result;

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class TypeResult<T> {
    private Supplier<T> value;
    private List<Supplier<TypeResult<?>>> causes;
    private BooleanSupplier success;
    private TypeOperation operation;

    public TypeResult(Supplier<T> value, List<Supplier<TypeResult<?>>> causes, BooleanSupplier success, TypeOperation operation) {
        this.value = value;
        this.causes = List.copyOf(causes);
        this.success = success;
        this.operation = operation;
    }

    public static <T> TypeResultBuilder<T> builder() {
        return new TypeResultBuilder<>();
    }

    public static <T> TypeResult<T> success(T value, TypeOperation operation) {
        return new TypeResult<>(() -> value, Collections.emptyList(), () -> true, operation);
    }

    public static <T> TypeResult<T> failure(TypeOperation operation) {
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

    public TypeOperation operation() {
        return this.operation;
    }

    public String buildMessage() {
        StringBuilder sb = new StringBuilder();
        buildMessage(sb, 0);
        return sb.toString();
    }

    private void buildMessage(StringBuilder sb, int indent) {
        String ind = "    ".repeat(indent);
        String ls = System.lineSeparator();
        boolean successful = this.successful();

        sb.append(ind).append("STATUS: ").append(successful ? "SUCCESS" : "FAILURE").append(ls);

        if (successful) {
            sb.append(ind).append("VALUE: ").append(this.value()).append(ls);
        }

        sb.append(ind).append("OPERATION: ").append(this.operation.getClass().getName()).append(ls);
        sb.append(ind).append("MESSAGE: ").append(this.operation.message());

        if (!this.causes.isEmpty()) {
            sb.append(ind).append("CAUSES: ").append(ls);
            for (TypeResult<?> result : this.causes()) {
                result.buildMessage(sb, indent + 1);
            }
        }
    }

}
