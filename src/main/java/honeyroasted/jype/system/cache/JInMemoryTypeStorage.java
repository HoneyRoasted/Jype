package honeyroasted.jype.system.cache;

import honeyroasted.jype.type.JType;

import java.util.HashMap;
import java.util.Map;

public class JInMemoryTypeStorage implements JTypeStorage {
    private Map<Class, JTypeCache> caches = new HashMap<>();
    private JTypeCacheFactory factory;

    public JInMemoryTypeStorage(JTypeCacheFactory factory) {
        this.factory = factory;
    }

    @Override
    public <K, T extends JType> JTypeCache<K, T> cacheFor(Class<?> keyType) {
        return this.caches.computeIfAbsent(keyType, this.factory::createCache);
    }
}
