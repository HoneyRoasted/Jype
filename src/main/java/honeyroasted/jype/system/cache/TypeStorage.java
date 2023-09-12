package honeyroasted.jype.system.cache;

import honeyroasted.jype.type.Type;

import java.util.HashMap;
import java.util.Map;

public class TypeStorage {
    private Map<Class, TypeCache> caches = new HashMap<>();
    private TypeCacheFactory factory;

    public TypeStorage(TypeCacheFactory factory) {
        this.factory = factory;
    }

    public <K, T extends Type> TypeCache<K, T> cacheFor(Class<?> keyType) {
        return this.caches.computeIfAbsent(keyType, this.factory::createCache);
    }
}
