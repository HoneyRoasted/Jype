package honeyroasted.jype.system.result.operations;

import honeyroasted.jype.Type;
import honeyroasted.jype.system.result.TypeOperation;

import java.util.Arrays;

public interface GeneralTypeOperations extends TypeOperation {

    class Kind extends AbstractTypeOperation implements TypeOperation {

        public Kind(Type type, Class<?>... targets) {
            super(String.format("%s:{%s} instance of %s",
                    (type == null ? null : type.kind()), type, Arrays.toString(targets)), type);
        }
    }

}
