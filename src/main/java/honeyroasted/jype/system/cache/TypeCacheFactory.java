package honeyroasted.jype.system.cache;

import honeyroasted.jype.type.Type;

public interface TypeCacheFactory {

    TypeCacheFactory IN_MEMORY_FACTORY = new TypeCacheFactory() {
        @Override
        public <K> TypeCache<K, Type> createCache(Class<K> keyType) {
            return new InMemoryTypeCache<>(keyType, Type.class);
        }
    };

    <K> TypeCache<K, Type> createCache(Class<K> keyType);

}
