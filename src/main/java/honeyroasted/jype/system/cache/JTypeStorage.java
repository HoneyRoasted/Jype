package honeyroasted.jype.system.cache;

import honeyroasted.jype.type.JType;

public interface JTypeStorage {
    <K, T extends JType> JTypeCache<K, T> cacheFor(Class<?> keyType);
}
