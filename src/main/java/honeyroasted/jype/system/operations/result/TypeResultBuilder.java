package honeyroasted.jype.system.operations.result;

import honeyroasted.jype.system.operations.TypeOperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
        } else if (this.success == null) {
            this.success = () -> true;
        }
        return this;
    }

    public TypeResultBuilder<T> and(Supplier<?> supplier) {
        Supplier<TypeResult<?>> res = make(supplier);
        this.causes.add(res);
        this.and(() -> res.get().successful());
        return this;
    }

    public TypeResultBuilder<T> and(TypeResult<?> result) {
        this.causes.add(() -> result);
        this.and(result::successful);
        return this;
    }

    public TypeResultBuilder<T> and(TypeOperation<?> operation) {
        this.causes.add(operation::perform);
        this.and(() -> operation.perform().successful());
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
        } else if (this.success == null) {
            this.success = () -> false;
        }
        return this;
    }

    public TypeResultBuilder<T> or(Supplier<?> supplier) {
        Supplier<TypeResult<?>> res = make(supplier);
        this.causes.add(res);
        this.or(() -> res.get().successful());
        return this;
    }

    public TypeResultBuilder<T> or(TypeResult<?> result) {
        this.causes.add(() -> result);
        this.or(result::successful);
        return this;
    }

    public TypeResultBuilder<T> or(TypeOperation<?> operation) {
        this.causes.add(operation::perform);
        this.or(() -> operation.perform().successful());
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

    private static Supplier<TypeResult<?>> make(Supplier<?> original) {
        return () -> {
            Object val = original.get();
            if (val instanceof TypeResult<?> res) {
                return res;
            } else if (val instanceof TypeOperation<?> op) {
                return op.perform();
            } else {
                throw new IllegalArgumentException("Expected Supplier<TypeResult<?>> or Supplier<TypeOperation<?>>, but got Supplier<"
                        + (val == null ? "null" : val.getClass().getName()) + "> instead");
            }
        };
    }
}
