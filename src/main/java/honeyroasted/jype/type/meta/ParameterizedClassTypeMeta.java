package honeyroasted.jype.type.meta;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.delegate.ParameterizedClassTypeDelegate;

import java.util.function.Function;

public class ParameterizedClassTypeMeta<T> extends ParameterizedClassTypeDelegate implements MetadataType<ParameterizedClassType, T> {
    private T metadata;

    public ParameterizedClassTypeMeta(TypeSystem system, Function<TypeSystem, ParameterizedClassType> factory) {
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
    public ParameterizedClassType stripMetadata() {
        return this.delegate().stripMetadata();
    }
}
