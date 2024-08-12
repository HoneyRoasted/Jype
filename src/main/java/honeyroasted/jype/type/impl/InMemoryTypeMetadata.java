package honeyroasted.jype.type.impl;

import honeyroasted.jype.type.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryTypeMetadata implements Type.Metadata {
    private Map<Class<?>, Object> metadata = new HashMap<>();

    public <T> Type.Metadata attach(Class<T> type, T data) {
        if (data != null) {
            this.metadata.put(type, data);
        }
        return this;
    }

    public <T> Type.Metadata detach(Class<T> type) {
        this.metadata.remove(type);
        return this;
    }

    public <T> Optional<T> get(Class<T> type) {
        return Optional.ofNullable((T) this.metadata.get(type));
    }

    @Override
    public <T> boolean has(Class<T> type) {
        return this.metadata.containsKey(type);
    }
}
