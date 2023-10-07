package honeyroasted.jype.type.meta;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.delegate.PrimitiveTypeDelegate;

import java.util.function.Function;

public class PrimitiveTypeMeta<T> extends PrimitiveTypeDelegate implements MetadataType<PrimitiveType, T> {
    private T metadata;

    public PrimitiveTypeMeta(TypeSystem system, Function<TypeSystem, PrimitiveType> factory) {
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
    public PrimitiveType stripMetadata() {
        return this.delegate().stripMetadata();
    }
}
