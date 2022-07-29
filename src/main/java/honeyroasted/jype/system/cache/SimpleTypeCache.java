package honeyroasted.jype.system.cache;

import honeyroasted.jype.Type;

import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.Map;

public class SimpleTypeCache implements TypeCache {
    private Map<Class<?>, Map<String, Type>> nameCache = new HashMap<>();
    private Map<Class<?>, Map<java.lang.reflect.Type, Object>> reflectCache = new HashMap<>();
    private Map<Class<?>, Map<TypeMirror, Object>> sourceCache = new HashMap<>();

    public <T extends Type> T get(String name, Class<T> clazz) {
        return (T) this.nameCache.computeIfAbsent(clazz, k -> new HashMap<>()).get(name);
    }

    public TypeCache cache(String name, Type type) {
        if (type != null) {
            this.nameCache.computeIfAbsent(type.getClass(), k -> new HashMap<>()).put(name, type);
        }
        return this;
    }

    public boolean has(String name, Class<? extends Type> clazz) {
        return this.nameCache.containsKey(clazz) && this.nameCache.get(clazz).containsKey(name);
    }

    @Override
    public <T extends Type> T get(java.lang.reflect.Type reflect, Class<T> clazz) {
        return (T) this.reflectCache.computeIfAbsent(clazz, k -> new HashMap<>()).get(reflect);
    }

    @Override
    public TypeCache cache(java.lang.reflect.Type reflect, Type type) {
        this.reflectCache.computeIfAbsent(type.getClass(), k -> new HashMap<>()).put(reflect, type);
        return this;
    }

    @Override
    public boolean has(java.lang.reflect.Type reflect, Class<? extends Type> clazz) {
        return this.reflectCache.containsKey(clazz) && this.reflectCache.get(clazz).containsKey(reflect);
    }

    @Override
    public <T extends Type> T get(TypeMirror source, Class<T> clazz) {
        return (T) this.sourceCache.computeIfAbsent(clazz, k -> new HashMap<>()).get(source);
    }

    @Override
    public TypeCache cache(TypeMirror source, Type type) {
        this.sourceCache.computeIfAbsent(type.getClass(), k -> new HashMap<>()).put(source, type);
        return this;
    }

    @Override
    public boolean has(TypeMirror source, Class<? extends Type> clazz) {
        return this.sourceCache.containsKey(clazz) && this.sourceCache.get(clazz).containsKey(source);
    }

}
