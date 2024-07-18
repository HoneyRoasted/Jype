package honeyroasted.jype.system.cache;

import honeyroasted.jype.type.Type;

import java.util.HashMap;
import java.util.Map;

public class InMemoryTypeStorage implements TypeStorage {
    private Map<Class, TypeCache> caches = new HashMap<>();
    private TypeCacheFactory factory;

    public InMemoryTypeStorage(TypeCacheFactory factory) {
        this.factory = factory;
    }

    @Override
    public <K, T extends Type> TypeCache<K, T> cacheFor(Class<?> keyType) {
        return this.caches.computeIfAbsent(keyType, this.factory::createCache);
    }
}
