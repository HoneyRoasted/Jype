package honeyroasted.jype.system.resolver.reflection;

public interface ReflectionType<T> {
    T type();
    record Type(java.lang.reflect.Type type) implements ReflectionType<java.lang.reflect.Type> {}
    record Executable(java.lang.reflect.Executable type) implements ReflectionType<java.lang.reflect.Executable> {}
}
