package honeyroasted.jype.system.result.operations;

import honeyroasted.jype.Type;
import honeyroasted.jype.system.result.TypeOperation;

import java.util.Collection;

public interface ConversionTypeOperations extends TypeOperation {

    class Identity extends AbstractTypeOperation implements ConversionTypeOperations {
        public Identity(Type from, Type to) {
            super(String.format("{%s} = {%s}", from, to), from, to);
        }
    }

}
