package honeyroasted.jype.location;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record JClassName(JType type, SubType subType, JClassName containing, String value) {

    public JClassName(JType type, SubType subType, String value) {
        this(type, subType, null, value);
    }

    public String simpleName() {
        if (this.containing != null && this.containing.type != JType.PACKAGE) {
            return this.containing.simpleName() + "." + this.value;
        } else {
            return this.value;
        }
    }

    public static JClassName of(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        if (clazz.isPrimitive()) {
            return new JClassName(JType.CLASS, SubType.NONE, clazz.getName());
        } else if (clazz.isArray()) {
            return new JClassName(JType.CLASS, SubType.ARRAY, of(clazz.getComponentType()), "[]");
        }

        JClassName containing;

        if (JClassName.isInInitializer(clazz)) {
            containing = new JClassName(JType.METHOD, SubType.INITIALIZER, of(clazz.getEnclosingClass()), "<initializer$" + nestIndex(clazz) + ">");
        } else if (clazz.getEnclosingMethod() != null) {
            containing = of(clazz.getEnclosingMethod());
        } else if (clazz.getEnclosingConstructor() != null) {
            containing = of(clazz.getEnclosingConstructor());
        } else if (clazz.getEnclosingClass() != null) {
            containing = of(clazz.getEnclosingClass());
        } else {
            containing = of(clazz.getPackage());
        }

        if (clazz.isAnonymousClass()) {
            return new JClassName(JType.CLASS, SubType.ANONYMOUS_CLASS, containing, "<anonymous$" + nestIndex(clazz) + ">");
        } else {
            return new JClassName(JType.CLASS, SubType.NONE, containing, clazz.getSimpleName());
        }
    }

    public static JClassName of(Method method) {
        if (method == null) {
            return null;
        }

        String simpleName = method.getName() + "(" + Stream.of(method.getParameterTypes()).map(c -> JClassName.of(c).toString()).collect(Collectors.joining(".")) + ")" +
                JClassName.of(method.getReturnType());
        return new JClassName(JType.METHOD, SubType.NONE, of(method.getDeclaringClass()), simpleName);
    }

    public static JClassName of(Constructor<?> constructor) {
        if (constructor == null) {
            return null;
        }

        String simpleName = "<constructor>(" + Stream.of(constructor.getParameterTypes()).map(c -> JClassName.of(c).toString()).collect(Collectors.joining(".")) + ")";
        return new JClassName(JType.METHOD, SubType.CONSTRUCTOR, of(constructor.getDeclaringClass()), simpleName);
    }

    public static JClassName of(Package pack) {
        if (pack == null) {
            return null;
        }

        return packageNameOf(pack.getName());
    }

    public static JClassName packageNameOf(String name) {
        if (name == null) {
            return null;
        }

        String[] arr = name.split("\\.");
        JClassName curr = new JClassName(JType.PACKAGE, SubType.NONE, arr[0]);

        for (int i = 1; i < arr.length; i++) {
            curr = new JClassName(JType.PACKAGE, SubType.NONE, curr, arr[i]);
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

    public boolean isArray() {
        return this.type == JType.CLASS && this.subType == SubType.ARRAY;
    }

    public boolean isAnonymousClass() {
        return this.type == JType.CLASS && this.subType == SubType.ANONYMOUS_CLASS;
    }

    public boolean isConstructor() {
        return this.type == JType.METHOD && this.subType == SubType.CONSTRUCTOR;
    }

    public boolean isReferenceable() {
        return !this.isAnonymousClass() &&
                (this.type == JType.CLASS || this.type == JType.PACKAGE) &&
                (this.containing == null || this.containing.isReferenceable());
    }

    public boolean isPackage() {
        return this.type == JType.PACKAGE &&
                (this.containing == null || this.containing.isPackage());
    }

    public String toString(String delim) {
        if (this.subType == SubType.ARRAY) {
            return this.containing == null ? this.value : this.containing.toString(delim) + this.value;
        } else {
            return this.containing == null ? this.value : this.containing.toString(delim) + delim + this.value;
        }
    }

    public String toString() {
        return this.toString(".");
    }

    enum JType {
        PACKAGE,
        CLASS,
        METHOD
    }

    enum SubType {
        INITIALIZER,
        CONSTRUCTOR,
        ANONYMOUS_CLASS,
        ARRAY,
        NONE
    }
}