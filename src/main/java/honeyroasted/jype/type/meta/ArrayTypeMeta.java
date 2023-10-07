package honeyroasted.jype.type.meta;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.delegate.ArrayTypeDelegate;

import java.util.function.Function;

public class ArrayTypeMeta<T> extends ArrayTypeDelegate implements MetadataType<ArrayType, T> {
    private T metadata;

    public ArrayTypeMeta(TypeSystem system, Function<TypeSystem, ArrayType> factory) {
        super(system, factory);
    }

    @Override
    public T metadata() {
        return this.metadata;
    }

    @Override
    public void setMetadata(T metadata) {
        this.metadata = metadata;
    }

    @Override
    public ArrayType stripMetadata() {
        return this.delegate().stripMetadata();
    }
}
