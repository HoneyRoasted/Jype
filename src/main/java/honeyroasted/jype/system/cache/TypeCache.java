package honeyroasted.jype.system.cache;

import honeyroasted.jype.Type;

import javax.lang.model.type.TypeMirror;

public interface TypeCache {

    <T extends Type> T get(String name, Class<T> clazz);

    TypeCache cache(String name, Type type);

    boolean has(String name, Class<? extends Type> clazz);

    <T extends Type> T get(java.lang.reflect.Type reflect, Class<T> clazz);

    TypeCache cache(java.lang.reflect.Type reflect, Type type);

    boolean has(java.lang.reflect.Type reflect, Class<? extends Type> clazz);

    <T extends Type> T get(TypeMirror source, Class<T> clazz);

    TypeCache cache(TypeMirror source, Type type);

    boolean has(TypeMirror source, Class<? extends Type> clazz);

}
