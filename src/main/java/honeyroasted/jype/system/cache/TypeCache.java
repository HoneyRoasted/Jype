package honeyroasted.jype.system.cache;

import honeyroasted.jype.type.Type;

import java.util.Optional;

public interface TypeCache<K, T extends Type> {

    boolean contains(K key);

    Optional<T> get(K key);

    void put(K key, T val);

}
