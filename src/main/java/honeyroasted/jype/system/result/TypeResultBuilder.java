package honeyroasted.jype.system.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class TypeResultBuilder<T> {
    private Supplier<T> value = () -> null;
    private BooleanSupplier success = null;
    private List<Supplier<TypeResult<?>>> causes = new ArrayList<>();
    private TypeOperation operation;

    public TypeResultBuilder<T> and(BooleanSupplier success) {
        if (this.success == null) {
            this.success = success;
        } else {
            this.success = () -> this.success.getAsBoolean() && success.getAsBoolean();
        }
        return this;
    }

    public TypeResultBuilder<T> and(boolean success) {
        if (!success) {
            this.success = () -> false;
        }
        return this;
    }

    public TypeResultBuilder<T> or(BooleanSupplier success) {
        if (this.success == null) {
            this.success = success;
        } else {
            this.success = () -> this.success.getAsBoolean() || success.getAsBoolean();
        }
        return this;
    }

    public TypeResultBuilder<T> or(boolean success) {
        if (success) {
            this.success = () -> true;
        }
        return this;
    }

    public TypeResultBuilder<T> setSuccess(BooleanSupplier success) {
        this.success = success;
        return this;
    }

    public TypeResultBuilder<T> setSuccess(boolean success) {
        this.success = () -> success;
        return this;
    }

    public TypeResultBuilder<T> causes(Supplier<TypeResult<?>>... causes) {
        Collections.addAll(this.causes, causes);
        return this;
    }

    public TypeResultBuilder<T> causes(TypeResult<?>... causes) {
        for (TypeResult<?> cause : causes) {
            this.causes.add(() -> cause);
        }
        return this;
    }

    public TypeResultBuilder<T> prerequisites(Supplier<TypeResult<?>>... causes) {
        for (Supplier<TypeResult<?>> cause : causes) {
            this.and(() -> cause.get().successful());
        }
        return this.causes(causes);
    }

    public TypeResultBuilder<T> prerequisites(TypeResult<?>... causes) {
        for (TypeResult<?> cause : causes) {
            this.and(cause::successful);
        }
        return this.causes(causes);
    }

    public TypeResultBuilder<T> operation(TypeOperation operation) {
        this.operation = operation;
        return this;
    }

    public TypeResultBuilder<T> value(Supplier<T> value) {
        this.value = value;
        return this;
    }

    public TypeResultBuilder<T> value(T value) {
        this.value = () -> value;
        return this;
    }

    public TypeResult<T> build() {
        return new TypeResult<>(this.value, this.causes, this.success, this.operation);
    }

}
