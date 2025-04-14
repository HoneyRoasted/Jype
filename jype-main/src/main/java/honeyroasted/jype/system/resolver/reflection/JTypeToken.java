package honeyroasted.jype.system.resolver.reflection;

import honeyroasted.jype.system.JTypeSystem;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class JTypeToken<T> {

    public final Type extractType() {
        return ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public final <K extends honeyroasted.jype.type.JType> K resolve(JTypeSystem system) {
        return (K) system.tryResolve(this);
    }

    public final <K extends honeyroasted.jype.type.JType> K resolve() {
        return this.resolve(JTypeSystem.RUNTIME_REFLECTION);
    }

    @Override
    public String toString() {
        return "JTypeToken{" + extractType() + "}";
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
