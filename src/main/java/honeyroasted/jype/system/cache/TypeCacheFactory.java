package honeyroasted.jype.system.cache;

import honeyroasted.jype.type.Type;

public interface TypeCacheFactory {

    TypeCacheFactory IN_MEMORY_FACTORY = new TypeCacheFactory() {
        @Override
        public <K, T extends Type> TypeCache<K, T> createCache(Class<K> keyType) {
            return new InMemoryTypeCache<>();
        }
    };

    <K, T extends Type> TypeCache<K, T> createCache(Class<K> keyType);

}
