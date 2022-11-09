package honeyroasted.jype.system.operations;

import honeyroasted.jype.Type;

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

    public <T extends Type> T type(int index) {
        return (T) this.types.get(index);
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
