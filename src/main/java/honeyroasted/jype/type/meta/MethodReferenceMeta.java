package honeyroasted.jype.type.meta;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.MethodReference;
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
}
