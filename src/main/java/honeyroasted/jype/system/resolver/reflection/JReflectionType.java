package honeyroasted.jype.system.resolver.reflection;

public interface JReflectionType<T> {
    T type();

    record Type(java.lang.reflect.Type type) implements JReflectionType<java.lang.reflect.Type> {
    }

    record Executable(java.lang.reflect.Executable type) implements JReflectionType<java.lang.reflect.Executable> {
    }

    record Field(java.lang.reflect.Field type) implements JReflectionType<java.lang.reflect.Field> {

    }

}
