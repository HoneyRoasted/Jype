package honeyroasted.jype.system.operations.result;

import honeyroasted.jype.system.operations.TypeOperation;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class TypeResultBuilder<T> {
    private Function<TypeResult<T>, T> value = t -> null;
    private List<Function<TypeResult<T>, TypeResult<?>>> causes;
    private Predicate<TypeResult<T>> success;
    private TypeOperation<T> operation;

    public TypeResultBuilder<T> and(Predicate<TypeResult<T>> success) {
        if (this.success == null) {
            this.success = success;
        } else {
            Predicate<TypeResult<T>> curr = this.success;
            this.success = t -> curr.test(t) && success.test(t);
        }
        return this;
    }

    public TypeResultBuilder<T> and(boolean success) {
        if (!success) {
            this.success = t -> false;
        } else if (this.success == null) {
            this.success = t -> true;
        }
        return this;
    }

    public TypeResultBuilder<T> andCause(Function<TypeResult<T>, ?> supplier) {
        Function<TypeResult<T>, TypeResult<?>> res = make(supplier);
        this.causes.add(res);
        this.and(t -> res.apply(t).successful());
        return this;
    }

    public TypeResultBuilder<T> andCause(TypeResult<?> result) {
        this.causes.add(t -> result);
        this.and(t -> result.successful());
        return this;
    }

    public TypeResultBuilder<T> andCause(TypeOperation<?> operation) {
        this.causes.add(t -> operation.perform());
        this.and(t -> operation.perform().successful());
        return this;
    }

    public TypeResultBuilder<T> or(Predicate<TypeResult<T>> success) {
        if (this.success == null) {
            this.success = success;
        } else {
            Predicate<TypeResult<T>> curr = this.success;
            this.success = t -> curr.test(t) || success.test(t);
        }
        return this;
    }

    public TypeResultBuilder<T> or(boolean success) {
        if (success) {
            this.success = t -> true;
        } else if (this.success == null) {
            this.success = t -> false;
        }
        return this;
    }

    public TypeResultBuilder<T> orCause(Function<TypeResult<T>, ?> supplier) {
        Function<TypeResult<T>, TypeResult<?>> res = make(supplier);
        this.causes.add(res);
        this.or(t -> res.apply(t).successful());
        return this;
    }

    public TypeResultBuilder<T> orCause(TypeResult<?> result) {
        this.causes.add(t -> result);
        this.or(t -> result.successful());
        return this;
    }

    public TypeResultBuilder<T> orCause(TypeOperation<?> operation) {
        this.causes.add(t -> operation.perform());
        this.or(t -> operation.perform().successful());
        return this;
    }

    public TypeResultBuilder<T> setSuccess(Predicate<TypeResult<T>> success) {
        this.success = success;
        return this;
    }

    public TypeResultBuilder<T> setSuccess(boolean success) {
        this.success = t -> success;
        return this;
    }

    public TypeResultBuilder<T> causes(Function<TypeResult<T>, TypeResult<?>>... causes) {
        Collections.addAll(this.causes, causes);
        return this;
    }

    public TypeResultBuilder<T> causes(TypeResult<?>... causes) {
        for (TypeResult<?> cause : causes) {
            this.causes.add(t -> cause);
        }
        return this;
    }

    public TypeResultBuilder<T> causes(TypeOperation<?>... operations) {
        for (TypeOperation<?> operation : operations) {
            this.causes.add(t -> operation.perform());
        }
        return this;
    }

    public TypeResultBuilder<T> operation(TypeOperation<T> operation) {
        this.operation = operation;
        return this;
    }

    public TypeResultBuilder<T> valueAttempt(Function<TypeResult<T>, T> value) {
        this.value(value);
        this.and(t -> value.apply(t) != null);
        return this;
    }

    public TypeResultBuilder<T> value(Function<TypeResult<T>, T> value) {
        this.value = value;
        return this;
    }

    public TypeResultBuilder<T> value(T value) {
        this.value = t -> value;
        return this;
    }

    public TypeResult<T> build() {
        return new TypeResult<>(this.value, this.causes, this.success, this.operation);
    }

    private Function<TypeResult<T>, TypeResult<?>> make(Function<TypeResult<T>, ?> original) {
        return t -> {
            Object val = original.apply(t);
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
