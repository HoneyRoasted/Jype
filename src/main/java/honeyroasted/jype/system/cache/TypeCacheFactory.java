package honeyroasted.jype.system.cache;

import honeyroasted.jype.type.Type;

public interface TypeCacheFactory {

    <K, T extends Type> TypeCache<K, T> createCache(Class<K> keyType);

}
