package honeyroasted.jype.type.delegate;

import honeyroasted.jype.type.Type;

public interface DelegateType<T extends Type> extends Type {

    T delegate();

    void expireDelegate();

}
