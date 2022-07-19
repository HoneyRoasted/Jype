package honeyroasted.jype.system;

import honeyroasted.jype.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TypeCache {
    private Map<Class<?>, Map<String, Type>> types = new HashMap<>();

    public Optional<Type> get(String name, Class<? extends Type> clazz) {
        return Optional.ofNullable(this.types.computeIfAbsent(clazz, k -> new HashMap<>()).get(name));
    }

    public TypeCache cache(String name, Type type) {
        if (type != null) {
            this.types.computeIfAbsent(type.getClass(), k -> new HashMap<>()).put(name, type);
        }
        return this;
    }

}
