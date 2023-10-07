package honeyroasted.jype.type.meta;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.WildType;
import honeyroasted.jype.type.delegate.WildTypeLowerDelegate;

import java.util.function.Function;

public class WildTypeLowerMeta<T> extends WildTypeLowerDelegate implements MetadataType<WildType.Lower, T> {
    private T metadata;

    public WildTypeLowerMeta(TypeSystem system, Function<TypeSystem, WildType.Lower> factory) {
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
    public WildType.Lower stripMetadata() {
        return this.delegate().stripMetadata();
    }
}
