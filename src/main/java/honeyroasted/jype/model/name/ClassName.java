package honeyroasted.jype.model.name;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ClassName(Type type, SubType subType, ClassName containing, String value) {

    public ClassName(Type type, SubType subType, String value) {
        this(type, subType, null, value);
    }

    public static ClassName of(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        if (clazz.isPrimitive()) {
            return new ClassName(Type.CLASS, SubType.NONE, clazz.getName());
        }

        ClassName containing;

        if (ClassName.isInInitializer(clazz)) {
            containing = new ClassName(Type.METHOD, SubType.INITIALIZER, of(clazz.getEnclosingClass()), "<initializer$" + nestIndex(clazz) + ">");
        } else if (clazz.getEnclosingMethod() != null) {
            containing = of(clazz.getEnclosingMethod());
        } else if (clazz.getEnclosingConstructor() != null) {
            containing = of(clazz.getEnclosingConstructor());
        } else if (clazz.getEnclosingClass() != null) {
            containing = of(clazz.getEnclosingClass());
        } else if (clazz.isArray()) {
            Class<?> curr = clazz;
            while (curr.isArray()) {
                curr = curr.getComponentType();
            }
            containing = of(curr.getPackage());
        } else {
            containing = of(clazz.getPackage());
        }

        if (clazz.isAnonymousClass()) {
            return new ClassName(Type.CLASS, SubType.ANONYMOUS_CLASS, containing, "<anonymous$" + nestIndex(clazz) + ">");
        } else {
            return new ClassName(Type.CLASS, SubType.NONE, containing, clazz.getSimpleName());
        }
    }

    public static ClassName of(Method method) {
        if (method == null) {
            return null;
        }

        String simpleName = method.getName() + "(" + Stream.of(method.getParameterTypes()).map(c -> ClassName.of(c).toString()).collect(Collectors.joining(".")) + ")" +
                ClassName.of(method.getReturnType());
        return new ClassName(Type.METHOD, SubType.NONE, of(method.getDeclaringClass()), simpleName);
    }

    public static ClassName of(Constructor<?> constructor) {
        if (constructor == null) {
            return null;
        }

        String simpleName = "<constructor>(" + Stream.of(constructor.getParameterTypes()).map(c -> ClassName.of(c).toString()).collect(Collectors.joining(".")) + ")";
        return new ClassName(Type.METHOD, SubType.CONSTRUCTOR, of(constructor.getDeclaringClass()), simpleName);
    }

    public static ClassName of(Package pack) {
        if (pack == null) {
            return null;
        }

        return packageNameOf(pack.getName());
    }

    public static ClassName packageNameOf(String name) {
        if (name == null) {
            return null;
        }

        String[] arr = name.split("\\.");
        ClassName curr = new ClassName(Type.PACKAGE, SubType.NONE, arr[0]);

        for (int i = 1; i < arr.length; i++) {
            curr = new ClassName(Type.PACKAGE, SubType.NONE, curr, arr[i]);
        }

        return curr;
    }

    private static boolean isInInitializer(Class<?> cls) {
        if (cls == null) {
            return false;
        }

        /*
         * Classes declared in a method or constructor report they do not have
         * a declaring class, but do report an enclosing class. A class declared
         * in a <clinit> initializer, or {} initializer block, behaves the same way,
         * but it also reports it does not have an enclosing method or constructor.
         * I haven't found any other way to check this.
         */

        return cls.getEnclosingMethod() == null &&
                cls.getEnclosingConstructor() == null &&
                cls.getDeclaringClass() == null &&
                cls.getEnclosingClass() != null;
    }

    private static int nestIndex(Class<?> cls) {
        return indexOf(cls, cls.getEnclosingClass().getNestMembers());
    }

    private static <T> int indexOf(T val, T[] arr) {
        for (int i = 0; i < arr.length; i++) {
            if (Objects.equals(arr[i], val)) {
                return i;
            }
        }
        return -1;
    }

    public boolean isAnonymousClass() {
        return this.type == Type.CLASS && this.subType == SubType.ANONYMOUS_CLASS;
    }

    public boolean isConstructor() {
        return this.type == Type.METHOD && this.subType == SubType.CONSTRUCTOR;
    }

    public boolean isReferenceable() {
        return !this.isAnonymousClass() &&
                (this.type == Type.CLASS || this.type == Type.PACKAGE) &&
                (this.containing == null || this.containing.isReferenceable());
    }

    public boolean isPackage() {
        return this.type == Type.PACKAGE &&
                (this.containing == null || this.containing.isPackage());
    }

    public String toString(String delim) {
        return this.containing == null ? this.value : this.containing.toString(delim) + delim + this.value;
    }

    public String toString() {
        return this.toString(".");
    }

    enum Type {
        PACKAGE,
        CLASS,
        METHOD
    }

    enum SubType {
        INITIALIZER,
        CONSTRUCTOR,
        ANONYMOUS_CLASS,
        NONE
    }
}
