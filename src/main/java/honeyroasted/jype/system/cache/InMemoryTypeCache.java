package honeyroasted.jype.system.cache;

import honeyroasted.jype.type.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryTypeCache<K, T extends Type> implements TypeCache<K, T> {
    private final Map<K, T> cache;

    public InMemoryTypeCache(Map<K, T> cache) {
        this.cache = cache;
    }

    public InMemoryTypeCache() {
        this(new HashMap<>());
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
}
