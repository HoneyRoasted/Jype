package honeyroasted.jype.type.meta;

import honeyroasted.jype.modify.Copyable;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.delegate.IntersectionTypeDelegate;

import java.util.function.Function;

public class IntersectionTypeMeta<T> extends IntersectionTypeDelegate implements MetadataType<IntersectionType, T> {
    private T metadata;

    public IntersectionTypeMeta(TypeSystem system, Function<TypeSystem, IntersectionType> factory) {
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
    public IntersectionType stripMetadata() {
        return this.delegate().stripMetadata();
    }

    @Override
    public <K extends Type> K copy(TypeCache<Type, Type> cache) {
        IntersectionTypeMeta<T> copy = new IntersectionTypeMeta<>(this.typeSystem(), t -> this.delegate().copy(cache));
        copy.setMetadata(this.metadata instanceof Copyable<?> cp ? (T) cp.copy(cache) : this.metadata);
        return (K) copy;
    }
}
