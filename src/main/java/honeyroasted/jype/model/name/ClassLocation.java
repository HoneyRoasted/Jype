package honeyroasted.jype.model.name;

public record ClassLocation(Type type, ClassLocation containing, String value) {
    public static final ClassLocation NONE = new ClassLocation(Type.CLASS, null, null);
    public static final ClassLocation DEFAULT_MODULE = new ClassLocation(Type.MODULE, null, null);

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
            ClassLocation curr = module.getName() == null ? DEFAULT_MODULE :
                    new ClassLocation(Type.MODULE, null, module.getName());

            if (pack != null) {
                String[] parts = pack.getName().split("\\.");

                for (String part : parts) {
                    curr = new ClassLocation(Type.PACKAGE, curr, part);
                }
            }
            return curr;
        }
    }

    public String toString(String delim, String moduleDelim) {
        StringBuilder sb = new StringBuilder();
        if (this.containing != null && !this.containing.equals(DEFAULT_MODULE)) {
            sb.append(this.containing);
            if (this.containing.type == Type.MODULE) {
                sb.append(moduleDelim);
            } else if (this.type != Type.ARRAY) {
                sb.append(delim);
            }
        }

        if (this.value != null) {
            sb.append(this.value);
        }

        return sb.toString();
    }

    public String toString() {
        return this.toString(".", "/");
    }

    enum Type {
        ARRAY,
        MODULE,
        PACKAGE,
        CLASS
    }

}
