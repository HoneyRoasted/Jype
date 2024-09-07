package honeyroasted.jype.system.cache;

import honeyroasted.jype.type.JType;

import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Optional;

public interface JTypeCache<K, T extends JType> {

    boolean contains(K key);

    Optional<T> get(K key);

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
