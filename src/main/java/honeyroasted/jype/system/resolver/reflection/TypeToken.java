package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.system.TypeSystem;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class TypeToken<T> {

    public final Type extractType() {
        return ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public <K extends honeyroasted.jype.type.Type> K resolve(TypeSystem system) {
        return (K) system.resolve(this).get();
    }

    public <K extends honeyroasted.jype.type.Type> K resolve() {
        return this.resolve(TypeSystem.RUNTIME);
    }

}
