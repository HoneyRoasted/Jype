package honeyroasted.jype.model.name;

public record ClassLocation(Type type, ClassLocation containing, String value) {
    public static final ClassLocation NONE = new ClassLocation(Type.CLASS, null, null);

    public static ClassLocation of(Class<?> cls) {
        if (cls == null) {
            return null;
        } else if (cls.isArray() || cls.isPrimitive()) {
            return ClassLocation.NONE;
        } else {
            String[] parts = cls.getName().split("\\.");
            return new ClassLocation(Type.CLASS, of(cls.getPackage()), parts[parts.length - 1]);
        }
    }

    public static ClassLocation of(Package pack) {
        if (pack == null) {
            return null;
        } else {
            String[] parts = pack.getName().split("\\.");
            ClassLocation curr = new ClassLocation(Type.PACKAGE, null, parts[0]);
            for (int i = 1; i < parts.length; i++) {
                curr = new ClassLocation(Type.PACKAGE, curr, parts[i]);
            }
            return curr;
        }
    }

    public String toString(String delim) {
        return this.containing == null ? this.value : this.containing.toString(delim) + delim + this.value;
    }

    public String toString() {
        return this.toString("/");
    }

    enum Type {
        PACKAGE,
        CLASS
    }

}
