package honeyroasted.jype.type.meta;

import honeyroasted.jype.modify.Copyable;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.delegate.DelegateType;
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

    @Override
    public <K extends Type> K copy(TypeCache<Type, Type> cache) {
        ParameterizedClassTypeMeta<T> copy = new ParameterizedClassTypeMeta<>(this.typeSystem(), t -> this.delegate().copy(cache));
        copy.setMetadata(this.metadata instanceof Copyable<?> cp ? (T) cp.copy(cache) : this.metadata);
        return (K) copy;
    }
}
