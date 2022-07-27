package honeyroasted.jype.system.cache;

import honeyroasted.jype.Type;

import java.util.HashMap;
import java.util.Map;

public class SimpleTypeCache implements TypeCache {
    private Map<Class<?>, Map<String, Type>> types = new HashMap<>();

    public <T extends Type> T get(String name, Class<T> clazz) {
        return (T) this.types.computeIfAbsent(clazz, k -> new HashMap<>()).get(name);
    }

    public TypeCache cache(String name, Type type) {
        if (type != null) {
            this.types.computeIfAbsent(type.getClass(), k -> new HashMap<>()).put(name, type);
        }
        return this;
    }

    public boolean has(String name, Class<? extends Type> clazz) {
        return this.types.containsKey(clazz) && this.types.get(clazz).containsKey(name);
    }

}
