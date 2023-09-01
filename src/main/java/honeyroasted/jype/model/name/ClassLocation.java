package honeyroasted.jype.model.name;

public record ClassLocation(Type type, ClassLocation containing, String value) {
    public static final ClassLocation NONE = new ClassLocation(Type.CLASS, null, null);

    public static ClassLocation of(Class<?> cls) {
        if (cls == null) {
            return null;
        } else if (cls.isArray()) {
            return new ClassLocation(Type.ARRAY, of(cls.getComponentType()), "[]");
        } else {
            String[] parts = cls.getName().split("\\.");
            return new ClassLocation(Type.CLASS, of(cls.getPackage(), cls.getModule()), parts[parts.length - 1]);
        }
    }

    public static ClassLocation of(Package pack, Module module) {
        if (pack == null && module == null) {
            return null;
        }  else {
            ClassLocation curr = new ClassLocation(Type.MODULE, null, module.getName() == null ? "<default>" : module.getName());

            if (pack != null) {
                String[] parts = pack.getName().split("\\.");

                for (String part : parts) {
                    curr = new ClassLocation(Type.PACKAGE, curr, part);
                }
            }
            return curr;
        }
    }

    public String toString(String delim) {
        if (this.type == Type.ARRAY) {
            return this.containing == null ? this.value : this.containing.toString(delim) + this.value;
        } else {
            return this.containing == null ? this.value : this.containing.toString(delim) + delim + this.value;
        }
    }

    public String toString() {
        return this.toString("/");
    }

    enum Type {
        ARRAY,
        MODULE,
        PACKAGE,
        CLASS
    }

}
