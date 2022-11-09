package honeyroasted.jype.system.operations;

import honeyroasted.jype.Type;
import honeyroasted.jype.system.operations.result.TypeResult;

import java.util.List;

public interface TypeOperation<T> {

    TypeResult<T> perform();

    List<Type> types();

    String message();

}
