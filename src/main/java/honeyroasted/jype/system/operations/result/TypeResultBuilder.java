package honeyroasted.jype.system.operations.result;

import honeyroasted.jype.system.operations.TypeOperation;

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
            BooleanSupplier curr = this.success;
            this.success = () -> curr.getAsBoolean() && success.getAsBoolean();
        }
        return this;
    }

    public TypeResultBuilder<T> and(boolean success) {
        if (!success) {
            this.success = () -> false;
        }
        return this;
    }

    public TypeResultBuilder<T> and(Supplier<TypeResult<?>> supplier) {
        this.causes.add(supplier);
        BooleanSupplier curr = this.success;
        this.success = () -> curr.getAsBoolean() && supplier.get().successful();
        return this;
    }

    public TypeResultBuilder<T> and(TypeResult<?> result) {
        this.causes.add(() -> result);
        BooleanSupplier curr = this.success;
        this.success = () -> curr.getAsBoolean() && result.successful();
        return this;
    }

    public TypeResultBuilder<T> and(TypeOperation<?> operation) {
        this.causes.add(operation::perform);
        BooleanSupplier curr = this.success;
        this.success = () -> curr.getAsBoolean() && operation.perform().successful();
        return this;
    }

    public TypeResultBuilder<T> or(BooleanSupplier success) {
        if (this.success == null) {
            this.success = success;
        } else {
            BooleanSupplier curr = this.success;
            this.success = () -> curr.getAsBoolean() || success.getAsBoolean();
        }
        return this;
    }

    public TypeResultBuilder<T> or(boolean success) {
        if (success) {
            this.success = () -> true;
        }
        return this;
    }

    public TypeResultBuilder<T> or(Supplier<TypeResult<?>> supplier) {
        this.causes.add(supplier);
        BooleanSupplier curr = this.success;
        this.success = () -> curr.getAsBoolean() || supplier.get().successful();
        return this;
    }

    public TypeResultBuilder<T> or(TypeResult<?> result) {
        this.causes.add(() -> result);
        BooleanSupplier curr = this.success;
        this.success = () -> curr.getAsBoolean() || result.successful();
        return this;
    }

    public TypeResultBuilder<T> or(TypeOperation<?> operation) {
        this.causes.add(operation::perform);
        BooleanSupplier curr = this.success;
        this.success = () -> curr.getAsBoolean() || operation.perform().successful();
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

    public TypeResultBuilder<T> causes(TypeOperation<?>... operations) {
        for (TypeOperation<?> operation : operations) {
            this.causes.add(operation::perform);
        }
        return this;
    }

    public TypeResultBuilder<T> operation(TypeOperation<T> operation) {
        this.operation = operation;
        return this;
    }

    public TypeResultBuilder<T> valueAttempt(Supplier<T> value) {
        this.value(value);
        this.and(() -> value.get() != null);
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
