package honeyroasted.jype.system.cache;

import honeyroasted.jype.type.JType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JInMemoryTypeCache<K, T extends JType> implements JTypeCache<K, T> {
    private final Map<K, T> cache;
    private Class<K> keyType;
    private Class<T> valueType;

    public JInMemoryTypeCache(Map<K, T> cache, Class<K> keyType, Class<T> valueType) {
        this.cache = cache;
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public JInMemoryTypeCache(Class<K> keyType, Class<T> valueType) {
        this(new HashMap<>(), keyType, valueType);
    }

    @Override
    public boolean contains(K key) {
        return this.cache.containsKey(key);
    }

    @Override
    public Optional<T> get(K key) {
        return Optional.ofNullable(this.cache.get(key));
    }

    @Override
    public void put(K key, T val) {
        this.cache.put(key, val);
    }

    @Override
    public void remove(K key) {
        this.cache.remove(key);
    }

    @Override
    public Map<K, T> asMap() {
        return new HashMap<>(this.cache);
    }
}
