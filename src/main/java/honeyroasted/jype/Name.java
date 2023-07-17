package honeyroasted.jype;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Name(Type type, SubType subType, Name containing, String value) {

    public Name(Type type, SubType subType, String value) {
        this(type, subType, null, value);
    }

    public static Name locationOf(Package pack) {
        return nameOf(pack);
    }

    public static Name locationOf(Method method) {
        if (method == null) {
            return null;
        }

        String simpleName = method.getName() + "(" + Stream.of(method.getParameterTypes()).map(c -> Name.locationOf(c).toString()).collect(Collectors.joining(".")) + ")" +
                Name.locationOf(method.getReturnType());
        return new Name(Type.METHOD, SubType.NONE, locationOf(method.getDeclaringClass()), simpleName);
    }

    public static Name locationOf(Constructor<?> constructor) {
        if (constructor == null) {
            return null;
        }

        String simpleName = "<constructor>(" + Stream.of(constructor.getParameterTypes()).map(c -> Name.locationOf(c).toString()).collect(Collectors.joining(".")) + ")";
        return new Name(Type.METHOD, SubType.CONSTRUCTOR, locationOf(constructor.getDeclaringClass()), simpleName);
    }

    public static Name locationOf(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        String[] parts = clazz.getName().split("\\.");
        return new Name(Type.CLASS, SubType.NONE, locationOf(clazz.getPackage()), parts[parts.length - 1]);
    }

    public static Name nameOf(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        if (clazz.isPrimitive()) {
            return new Name(Type.CLASS, SubType.NONE, clazz.getName());
        }

        Name containing;

        if (Name.isInInitializer(clazz)) {
            if (Name.isInConstructorInitializer(clazz)) {
                containing = new Name(Type.METHOD, SubType.INITIALIZER, nameOf(clazz.getEnclosingClass()), "<init$" + nestIndex(clazz) + ">");
            } else {
                containing = new Name(Type.METHOD, SubType.STATIC_INITIALIZER, nameOf(clazz.getEnclosingClass()), "<clinit$" + nestIndex(clazz) + ">");
            }
        } else if (clazz.getEnclosingMethod() != null) {
            containing = nameOf(clazz.getEnclosingMethod());
        } else if (clazz.getEnclosingConstructor() != null) {
            containing = nameOf(clazz.getEnclosingConstructor());
        } else if (clazz.getEnclosingClass() != null) {
            containing = nameOf(clazz.getEnclosingClass());
        } else if (clazz.isArray()) {
            Class<?> curr = clazz;
            while (curr.isArray()) {
                curr = curr.getComponentType();
            }
            containing = nameOf(curr.getPackage());
        } else {
            containing = nameOf(clazz.getPackage());
        }

        if (clazz.isAnonymousClass()) {
            return new Name(Type.CLASS, SubType.ANONYMOUS_CLASS, containing, "<anonymous$" + nestIndex(clazz) + ">");
        } else {
            return new Name(Type.CLASS, SubType.NONE, containing, clazz.getSimpleName());
        }
    }

    public static Name nameOf(Method method) {
        if (method == null) {
            return null;
        }

        String simpleName = method.getName() + "(" + Stream.of(method.getParameterTypes()).map(c -> Name.nameOf(c).toString()).collect(Collectors.joining(".")) + ")" +
                Name.nameOf(method.getReturnType());
        return new Name(Type.METHOD, SubType.NONE, nameOf(method.getDeclaringClass()), simpleName);
    }

    public static Name nameOf(Constructor<?> constructor) {
        if (constructor == null) {
            return null;
        }

        String simpleName = "<constructor>(" + Stream.of(constructor.getParameterTypes()).map(c -> Name.nameOf(c).toString()).collect(Collectors.joining(".")) + ")";
        return new Name(Type.METHOD, SubType.CONSTRUCTOR, nameOf(constructor.getDeclaringClass()), simpleName);
    }

    public static Name nameOf(Package pack) {
        if (pack == null) {
            return null;
        }

        return packageNameOf(pack.getName());
    }

    public static Name packageNameOf(String name) {
        if (name == null) {
            return null;
        }

        String[] arr = name.split("\\.");
        Name curr = new Name(Type.PACKAGE, SubType.NONE, arr[0]);

        for (int i = 1; i < arr.length; i++) {
            curr = new Name(Type.PACKAGE, SubType.NONE, curr, arr[i]);
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

    private static boolean isInConstructorInitializer(Class<?> cls) {
        /*
         * As far as I can tell, the only way to distinguish a class declared in a
         * <clinit> initializer vs an {} initializer block is that the class in the
         * <clinit> block will be static and not require an instance of the parent
         * class. I can't find any difference in modifiers or definitions of these
         * classes except that the class in the {} initializer block will have
         * it's enclosing class as a parameter in its constructor.
         */

        Class<?> enclosing = cls.getEnclosingClass();
        return Stream.of(cls.getDeclaredConstructors()).allMatch(c -> c.getParameterTypes().length > 0 && c.getParameterTypes()[0].equals(enclosing));
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
        return this.type == Type.METHOD && this.value.startsWith("<init>(");
    }

    public boolean isConstructorInitializer() {
        return this.type == Type.METHOD && this.value.equals("<init>");
    }

    public boolean isStaticInitializer() {
        return this.type == Type.METHOD && this.value.equals("<clinit>");
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

    public boolean isValidClassLocation() {
        return this.type == Type.CLASS && !this.isAnonymousClass() &&
                (this.containing == null || this.containing.isPackage());
    }

    public boolean isValidMethodLocation() {
        return this.type == Type.METHOD && this.containing != null && this.containing.isValidClassLocation();
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
        STATIC_INITIALIZER,
        CONSTRUCTOR,
        ANONYMOUS_CLASS,
        NONE
    }
}
