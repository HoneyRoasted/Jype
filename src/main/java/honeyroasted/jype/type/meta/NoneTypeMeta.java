package honeyroasted.jype.type.meta;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.delegate.NoneTypeDelegate;

import java.util.function.Function;

public class NoneTypeMeta<T> extends NoneTypeDelegate implements MetadataType<NoneType, T> {
    private T metadata;

    public NoneTypeMeta(TypeSystem system, Function<TypeSystem, NoneType> factory) {
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
    public NoneType stripMetadata() {
        return this.delegate().stripMetadata();
    }
}
