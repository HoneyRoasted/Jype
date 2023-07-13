package honeyroasted.jype;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record Name(Type type, Name containing, String value) {

    public Name(Type type, String value) {
        this(type, null, value);
    }

    public static Name locationOf(Package pack) {
        return nameOf(pack);
    }

    public static Name locationOf(Method method) {
        if (method == null) {
            return null;
        }

        String simpleName = method.getName() + "(" + Stream.of(method.getParameterTypes()).map(c -> Name.nameOf(c).toString()).collect(Collectors.joining(".")) + ")";
        return new Name(Type.METHOD, locationOf(method.getDeclaringClass()), simpleName);
    }

    public static Name locationOf(Constructor<?> constructor) {
        if (constructor == null) {
            return null;
        }

        String simpleName = "<init>(" + Stream.of(constructor.getParameterTypes()).map(c -> Name.nameOf(c).toString()).collect(Collectors.joining(".")) + ")";
        return new Name(Type.METHOD, locationOf(constructor.getDeclaringClass()), simpleName);
    }

    public static Name locationOf(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        String[] parts = clazz.getName().split("\\.");
        return new Name(Type.CLASS, locationOf(clazz.getPackage()), parts[parts.length - 1]);
    }

    public static Name nameOf(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        if (clazz.isPrimitive()) {
            return new Name(Type.CLASS, clazz.getName());
        }

        Name containing;

        if (Name.isInStaticInit(clazz)) {
            containing = new Name(Type.METHOD, nameOf(clazz.getEnclosingClass()), "<clinit>()");
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
            return new Name(Type.CLASS, containing, null);
        } else {
            return new Name(Type.CLASS, containing, clazz.getSimpleName());
        }
    }

    public static Name nameOf(Method method) {
        if (method == null) {
            return null;
        }

        String simpleName = method.getName() + "(" + Stream.of(method.getParameterTypes()).map(c -> Name.nameOf(c).toString()).collect(Collectors.joining(".")) + ")";
        return new Name(Type.METHOD, nameOf(method.getDeclaringClass()), simpleName);
    }

    public static Name nameOf(Constructor<?> constructor) {
        if (constructor == null) {
            return null;
        }

        String simpleName = "<init>(" + Stream.of(constructor.getParameterTypes()).map(c -> Name.nameOf(c).toString()).collect(Collectors.joining(".")) + ")";
        return new Name(Type.METHOD, nameOf(constructor.getDeclaringClass()), simpleName);
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
        Name curr = new Name(Type.PACKAGE, arr[0]);

        for (int i = 1; i < arr.length; i++) {
            curr = new Name(Type.PACKAGE, curr, arr[i]);
        }

        return curr;
    }

    private static boolean isInStaticInit(Class<?> cls) {
        if (cls == null) {
            return false;
        }

        String[] parts = cls.getName().split("\\$");
        String last = parts[parts.length - 1];
        /*
         * This is hacky and unreliable, but essentially, when a
         * class is declared inside the static initializer of another
         * class, Java will use the naming conventions applied to
         * classes declared in methods, but will not report
         * any enclosing method. Unsure why. I can't find
         * anything in bytecode or reflection to test
         * this in a more reliable way.
         */
        return cls.getEnclosingClass() != null &&
                cls.getEnclosingMethod() == null &&
                !last.isEmpty() &&
                Character.isDigit(last.charAt(0));
    }

    public boolean isAnonymousClass() {
        return this.type == Type.CLASS && this.value == null;
    }

    public boolean isConstructor() {
        return this.type == Type.METHOD && this.value.startsWith("<init>");
    }

    public boolean isStaticInit() {
        return this.type == Type.METHOD && this.value.startsWith("<clinit>");
    }

    public String toString(String delim) {
        if (this.isAnonymousClass()) {
            return this.containing == null ? "$#" : this.containing + "$#";
        }

        return this.containing == null ? this.value : this.containing +
                (this.containing.isAnonymousClass() ? "" : delim) + this.value;
    }

    public String toString() {
        return this.toString(".");
    }

    enum Type {
        PACKAGE,
        CLASS,
        METHOD
    }
}
