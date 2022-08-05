package honeyroasted.jype.system.cache;

import honeyroasted.jype.Type;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleTypeCache<K> implements TypeCache<K> {
    private Map<Class<?>, Map<K, Type>> map = new LinkedHashMap<>();

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
            this.map.computeIfAbsent(type.getClass(), k -> new HashMap<>()).put(key, type);
        }
        return this;
    }

    @Override
    public boolean has(K key, Class<? extends Type> clazz) {
        return this.map.containsKey(clazz) && this.map.get(clazz).containsKey(key);
    }
}
