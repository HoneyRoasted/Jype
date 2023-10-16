package honeyroasted.jype.type.meta;

import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.delegate.DelegateType;

public interface MetadataType<T extends Type, M> extends DelegateType<T> {

    default boolean hasMetadata(Class<?> type) {
        return type.isInstance(this.metadata()) || (this.delegate() instanceof MetadataType<?,?> mt && mt.hasMetadata(type));
    }

    default <K> K getMetadata(Class<K> type) {
        return type.isInstance(this.metadata()) ? (K) this.metadata() : (this.delegate() instanceof MetadataType<?,?> mt ? mt.getMetadata(type) : null);
    }

    M metadata();

    void setMetadata(M metadata);

    T stripMetadata();

}
