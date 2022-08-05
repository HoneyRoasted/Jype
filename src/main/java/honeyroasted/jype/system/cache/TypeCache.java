package honeyroasted.jype.system.cache;

import honeyroasted.jype.Type;

public interface TypeCache<K> {

    <T extends Type> T get(K key, Class<T> clazz);

    TypeCache<K> cache(K key, Type type);

    boolean has(K key, Class<? extends Type> clazz);

}
