package honeyroasted.jype.system.cache;

import honeyroasted.jype.type.Type;

public interface TypeStorage {
    <K, T extends Type> TypeCache<K, T> cacheFor(Class<?> keyType);
}
