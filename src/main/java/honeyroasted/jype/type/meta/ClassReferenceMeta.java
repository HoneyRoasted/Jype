package honeyroasted.jype.type.meta;

import honeyroasted.jype.modify.Copyable;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.delegate.ClassReferenceDelegate;

import java.util.function.Function;

public class ClassReferenceMeta<T> extends ClassReferenceDelegate implements MetadataType<ClassReference, T> {
    private T metadata;

    public ClassReferenceMeta(TypeSystem system, Function<TypeSystem, ClassReference> factory) {
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
    public ClassReference stripMetadata() {
        return this.delegate().stripMetadata();
    }

    @Override
    public <K extends Type> K copy(TypeCache<Type, Type> cache) {
        ClassReferenceMeta<T> copy = new ClassReferenceMeta<>(this.typeSystem(), t -> this.delegate().copy(cache));
        copy.setMetadata(this.metadata instanceof Copyable<?> cp ? (T) cp.copy(cache) : this.metadata);
        return (K) copy;
    }
}
