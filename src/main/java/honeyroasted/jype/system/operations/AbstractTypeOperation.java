package honeyroasted.jype.system.operations;

import honeyroasted.jype.Type;
import honeyroasted.jype.system.operations.result.TypeResult;

import java.util.Collection;
import java.util.List;

public abstract class AbstractTypeOperation<T> implements TypeOperation<T> {
    protected List<Type> types;
    protected String message;

    public AbstractTypeOperation(String message, Collection<Type> types) {
        this.types = List.copyOf(types);
        this.message = message;
    }

    public AbstractTypeOperation(String message, Type... types) {
        this.types = List.of(types);
        this.message = message;
    }

    protected <T extends Type> T type(int index) {
        return (T) this.types.get(index);
    }

    protected <T extends Type> T type() {
        return this.type(0);
    }

    protected T value() {
        return null;
    }

    protected TypeResult<T> result() {
        T val = this.value();
        if (val == null || Boolean.FALSE.equals(val)) {
            return TypeResult.failure(this);
        } else {
            return TypeResult.success(val, this);
        }
    }

    private TypeResult<T> cached = null;
    @Override
    public final TypeResult<T> perform() {
        if (this.cached == null) {
            this.cached = this.result();
        }

        return this.cached;
    }

    @Override
    public List<Type> types() {
        return this.types;
    }

    @Override
    public String message() {
        return this.message;
    }
}
