package honeyroasted.jype.system.cache;

import honeyroasted.jype.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * This class is a simple implementation of a {@link TypeCache}. It stores a mapping of classes and keys to
 * cached types. No attempt is made to remove items from the cache by this class, but the behavior of the
 * map can be customised by providing a factory for constructing maps.
 *
 * @param <K> The key type supported by this {@link TypeCache}
 */
public class SimpleTypeCache<K> implements TypeCache<K> {
    private Map<Class<?>, Map<K, Type>> map = new HashMap<>();
    private Supplier<Map<K, Type>> factory;

    /**
     * Creates a new {@link SimpleTypeCache} with the given factory for creating key to type maps. The created maps
     * may have any desired custom behavior, such as weak value references.
     *
     * @param factory The factory to use when creating new maps
     */
    public SimpleTypeCache(Supplier<Map<K, Type>> factory) {
        this.factory = factory;
    }

    /**
     * Creates a new {@link SimpleTypeCache} with no custom behavior. Any cached item will be held
     * until this {@link SimpleTypeCache} itself is garbage collected.
     */
    public SimpleTypeCache() {
        this(HashMap::new);
    }

    @Override
    public <T extends Type> T get(K key, Class<T> clazz) {
        Map<K, Type> subMap = this.map.get(clazz);
        if (subMap != null) {
            return (T) subMap.get(key);
        }
        return null;
    }

    @Override
    public TypeCache<K> cache(K key, Type type) {
        if (type != null) {
            this.map.computeIfAbsent(type.getClass(), k -> this.factory.get()).put(key, type);
        }
        return this;
    }

    @Override
    public boolean has(K key, Class<? extends Type> clazz) {
        Map<K, Type> subMap = this.map.get(clazz);
        if (subMap != null) {
            return subMap.containsKey(key);
        }
        return false;
    }
}
