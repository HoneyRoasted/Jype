package honeyroasted.jype.system.cache;

import honeyroasted.jype.system.resolver.JResolutionResult;
import honeyroasted.jype.type.JType;

import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Optional;

public interface JTypeCache<K, T extends JType> {

    boolean contains(K key);

    Optional<T> get(K key);

    default <Z extends T> JResolutionResult<K, Z> asResolution(K key) {
        return (JResolutionResult<K, Z>) JResolutionResult.inherit(key, get(key), "Cache lookup failed");
    }

    void put(K key, T val);

    void remove(K key);

    Map<K, T> asMap();

    default Class<K> keyType() {
        return (Class) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    default Class<T> valueType() {
        return (Class) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    }


}
