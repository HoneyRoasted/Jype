package honeyroasted.jype.type.impl;

import honeyroasted.jype.modify.Copyable;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class InMemoryTypeMetadata implements Type.Metadata {
    private Map<Class<?>, Object> metadata = new HashMap<>();

    @Override
    public <T> Type.Metadata attach(Class<? extends T> type, T data) {
        this.metadata.put(type, data);
        return this;
    }

    @Override
    public Type.Metadata detach(Class<?> type) {
        this.metadata.remove(type);
        return this;
    }

    @Override
    public Type.Metadata detachAll() {
        this.metadata.clear();
        return this;
    }

    @Override
    public <T> Optional<T> get(Class<? extends T> type) {
        return (Optional<T>) Optional.ofNullable(this.metadata.get(type));
    }

    @Override
    public boolean has(Class<?> type) {
        return this.metadata.containsKey(type);
    }

    @Override
    public Set<Class<?>> allMetadata() {
        return this.metadata.keySet();
    }

    @Override
    public Type.Metadata copyFrom(Type.Metadata other, TypeCache<Type, Type> cache) {
        this.metadata.clear();
        other.allMetadata().forEach(c -> {
            Object data = other.get(c).get();
            if (data instanceof Copyable<?> cp) {
                this.attach(c, cp.copy(cache));
            } else {
                this.attach(c, data);
            }
        });
        return this;
    }

    @Override
    public <T extends Type.Metadata> T copy(TypeCache<Type, Type> cache) {
        Type.Metadata meta = new InMemoryTypeMetadata();
        meta.copyFrom(this, cache);
        return (T) meta;
    }
}
