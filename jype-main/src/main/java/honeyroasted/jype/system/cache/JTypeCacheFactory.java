package honeyroasted.jype.system.cache;

import honeyroasted.jype.type.JType;

public interface JTypeCacheFactory {

    JTypeCacheFactory IN_MEMORY_FACTORY = new JTypeCacheFactory() {
        @Override
        public <K> JTypeCache<K, JType> createCache(Class<K> keyType) {
            return new JInMemoryTypeCache<>(keyType, JType.class);
        }
    };

    <K> JTypeCache<K, JType> createCache(Class<K> keyType);

}
