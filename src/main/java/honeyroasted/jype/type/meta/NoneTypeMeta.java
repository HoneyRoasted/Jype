package honeyroasted.jype.type.meta;

import honeyroasted.jype.modify.Copyable;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.Type;
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

    @Override
    public <K extends Type> K copy(TypeCache<Type, Type> cache) {
        NoneTypeMeta<T> copy = new NoneTypeMeta<>(this.typeSystem(), t -> this.delegate().copy(cache));
        copy.setMetadata(this.metadata instanceof Copyable<?> cp ? (T) cp.copy(cache) : this.metadata);
        return (K) copy;
    }
}
