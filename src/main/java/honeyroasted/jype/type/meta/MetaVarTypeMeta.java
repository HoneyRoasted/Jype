package honeyroasted.jype.type.meta;

import honeyroasted.jype.modify.Copyable;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.delegate.MetaVarTypeDelegate;

import java.util.function.Function;

public class MetaVarTypeMeta<T> extends MetaVarTypeDelegate implements MetadataType<MetaVarType, T> {
    private T metadata;

    public MetaVarTypeMeta(TypeSystem system, Function<TypeSystem, MetaVarType> factory) {
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
    public MetaVarType stripMetadata() {
        return this.delegate().stripMetadata();
    }

    @Override
    public <K extends Type> K copy(TypeCache<Type, Type> cache) {
        MetaVarTypeMeta<T> copy = new MetaVarTypeMeta<>(this.typeSystem(), t -> this.delegate().copy(cache));
        copy.setMetadata(this.metadata instanceof Copyable<?> cp ? (T) cp.copy(cache) : this.metadata);
        return (K) copy;
    }
}
