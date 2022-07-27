package honeyroasted.jype.system.cache;

import honeyroasted.jype.Type;

public interface TypeCache {

    <T extends Type> T get(String name, Class<T> clazz);

    TypeCache cache(String name, Type type);

    boolean has(String name, Class<? extends Type> clazz);

}
