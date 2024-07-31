package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.system.TypeSystem;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class TypeToken<T> {

    public final Type extractType() {
        return ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public final <K extends honeyroasted.jype.type.Type> K resolve(TypeSystem system) {
        return (K) system.tryResolve(this);
    }

    public final <K extends honeyroasted.jype.type.Type> K resolve() {
        return this.resolve(TypeSystem.SIMPLE_RUNTIME);
    }

    @Override
    public String toString() {
        return "TypeToken{" + extractType() + "}";
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && getClass().equals(obj.getClass());
    }
}
