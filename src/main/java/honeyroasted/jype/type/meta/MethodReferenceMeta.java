package honeyroasted.jype.type.meta;

import honeyroasted.jype.modify.Copyable;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.delegate.DelegateType;
import honeyroasted.jype.type.delegate.MethodReferenceDelegate;

import java.util.function.Function;

public class MethodReferenceMeta<T> extends MethodReferenceDelegate implements MetadataType<MethodReference, T> {
    private T metadata;

    public MethodReferenceMeta(TypeSystem system, Function<TypeSystem, MethodReference> factory) {
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
    public MethodReference stripMetadata() {
        return this.delegate().stripMetadata();
    }

    @Override
    public <K extends Type> K copy(TypeCache<Type, Type> cache) {
        MethodReferenceMeta<T> copy = new MethodReferenceMeta<>(this.typeSystem(), t -> this.delegate().copy(cache));
        copy.setMetadata(this.metadata instanceof Copyable<?> cp ? (T) cp.copy(cache) : this.metadata);
        return (K) copy;
    }
}
