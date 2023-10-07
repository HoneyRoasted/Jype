package honeyroasted.jype.type.meta;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.ParameterizedMethodType;
import honeyroasted.jype.type.delegate.ParameterizedMethodTypeDelegate;

import java.util.function.Function;

public class ParameterizedMethodTypeMeta<T> extends ParameterizedMethodTypeDelegate implements MetadataType<ParameterizedMethodType, T> {
    private T metadata;

    public ParameterizedMethodTypeMeta(TypeSystem system, Function<TypeSystem, ParameterizedMethodType> factory) {
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
    public ParameterizedMethodType stripMetadata() {
        return this.delegate().stripMetadata();
    }
}
