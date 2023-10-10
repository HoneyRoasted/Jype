package honeyroasted.jype.type.meta;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public interface MetadataType<T extends Type, M> extends Type {

    static <K extends Type> Function<TypeSystem, K> delayAndCache(Function<TypeSystem, K> fn) {
        return new Function<>() {
            private K val;

            @Override
            public K apply(TypeSystem system) {
                if (this.val == null) this.val = fn.apply(system);
                return this.val;
            }
        };
    }

    M metadata();

    void setMetadata(M metadata);

    T stripMetadata();

}
