package honeyroasted.jype.type.meta;

import honeyroasted.jype.modify.Copyable;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;
import honeyroasted.jype.type.delegate.DelegateType;
import honeyroasted.jype.type.delegate.WildTypeUpperDelegate;

import java.util.function.Function;

public class WildTypeUpperMeta<T> extends WildTypeUpperDelegate implements MetadataType<WildType.Upper, T> {
    private T metadata;

    public WildTypeUpperMeta(TypeSystem system, Function<TypeSystem, WildType.Upper> factory) {
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
    public WildType.Upper stripMetadata() {
        return this.delegate().stripMetadata();
    }

    @Override
    public <K extends Type> K copy(TypeCache<Type, Type> cache) {
        WildTypeUpperMeta<T> copy = new WildTypeUpperMeta<>(this.typeSystem(), DelegateType.delayAndCache(t -> this.delegate().copy(cache)));
        copy.setMetadata(this.metadata instanceof Copyable<?> cp ? (T) cp.copy(cache) : this.metadata);
        return (K) copy;
    }
}
