package honeyroasted.jype.system.operations;

import honeyroasted.jype.Type;
import honeyroasted.jype.system.operations.result.TypeResult;

import java.util.List;

public interface TypeOperation<T> {

    default T value() {
        return null;
    }

    default TypeResult<T> perform() {
        T value = this.value();
        if (value == null || Boolean.FALSE.equals(value)) {
            return TypeResult.failure(this);
        } else {
            return TypeResult.success(this.value(), this);
        }
    }

    List<Type> types();

    String message();

}
