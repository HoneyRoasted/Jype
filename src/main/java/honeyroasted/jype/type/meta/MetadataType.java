package honeyroasted.jype.type.meta;

import honeyroasted.jype.type.Type;

public interface MetadataType<T extends Type, M> extends Type {

    M metadata();

    void setMetadata(M metadata);

    T stripMetadata();

}
