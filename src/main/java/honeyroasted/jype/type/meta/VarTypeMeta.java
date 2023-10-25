package honeyroasted.jype.type.meta;

import honeyroasted.jype.modify.Copyable;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.delegate.DelegateType;
import honeyroasted.jype.type.delegate.VarTypeDelegate;

import java.util.function.Function;

public class VarTypeMeta<T> extends VarTypeDelegate implements MetadataType<VarType, T> {
    private T metadata;

    public VarTypeMeta(TypeSystem system, Function<TypeSystem, VarType> factory) {
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
    public VarType stripMetadata() {
        return this.delegate().stripMetadata();
    }

    @Override
    public <K extends Type> K copy(TypeCache<Type, Type> cache) {
        VarTypeMeta<T> copy = new VarTypeMeta<>(this.typeSystem(), t -> this.delegate().copy(cache));
        copy.setMetadata(this.metadata instanceof Copyable<?> cp ? (T) cp.copy(cache) : this.metadata);
        return (K) copy;
    }
}
