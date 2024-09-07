package honeyroasted.jype.system.resolver.reflection;

public interface JReflectionType<T> {
    T type();

    record Type(java.lang.reflect.Type type) implements JReflectionType<java.lang.reflect.Type> {
    }

    record Executable(java.lang.reflect.Executable type) implements JReflectionType<java.lang.reflect.Executable> {
    }
}
