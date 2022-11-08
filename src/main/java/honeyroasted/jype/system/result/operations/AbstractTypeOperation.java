package honeyroasted.jype.system.result.operations;

import honeyroasted.jype.Type;
import honeyroasted.jype.system.result.TypeOperation;

import java.util.Collection;
import java.util.List;

public class AbstractTypeOperation implements TypeOperation {
    private List<Type> types;
    private String message;

    public AbstractTypeOperation(String message, Collection<Type> types) {
        this.types = List.copyOf(types);
        this.message = message;
    }

    public AbstractTypeOperation(String message, Type... types) {
        this.types = List.of(types);
        this.message = message;
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
