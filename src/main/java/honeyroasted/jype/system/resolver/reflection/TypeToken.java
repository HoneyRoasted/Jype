package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.system.TypeSystem;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class TypeToken<T> {

    public final Type extractType() {
        return ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public honeyroasted.jype.type.Type resolve(TypeSystem system) {
        return system.resolve(this).get();
    }

    public honeyroasted.jype.type.Type resolve() {
        return this.resolve(TypeSystem.RUNTIME);
    }

}
