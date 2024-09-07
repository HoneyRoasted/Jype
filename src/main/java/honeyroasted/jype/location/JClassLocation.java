package honeyroasted.jype.location;

public record JClassLocation(Type type, JClassLocation containing, String value) {
    public static final JClassLocation DEFAULT_MODULE = new JClassLocation(Type.MODULE, null, null);
    public static final JClassLocation UNKNOWN_MODULE = new JClassLocation(Type.MODULE, null, null);
    public static final JClassLocation VOID = JClassLocation.of(void.class);

    public static JClassLocation of(String internalName) {
        return of(UNKNOWN_MODULE, internalName);
    }

    public static JClassLocation of(JClassLocation module, String internalName) {
        String[] parts = internalName.split("/");
        if (parts.length == 1) {
            return new JClassLocation(Type.CLASS, module, parts[0]);
        } else {
            JClassLocation result = new JClassLocation(Type.PACKAGE, module, parts[0]);
            for (int i = 1; i < parts.length - 1; i++) {
                result = new JClassLocation(Type.PACKAGE, result, parts[i]);
            }
            return new JClassLocation(Type.CLASS, result, parts[parts.length - 1]);
        }
    }

    public static JClassLocation of(Class<?> cls) {
        if (cls == null) return null;

        if (cls.isArray()) {
            return new JClassLocation(Type.ARRAY, of(cls.getComponentType()), "[]");
        } else if (cls.isPrimitive()) {
            return new JClassLocation(Type.CLASS, DEFAULT_MODULE, cls.getName());
        } else {
            String[] parts = cls.getName().split("\\.");
            return new JClassLocation(Type.CLASS, of(cls.getPackage(), cls.getModule()), parts[parts.length - 1]);
        }
    }

    public static JClassLocation of(Package pack, Module module) {
        if (pack == null && module == null) {
            return null;
        } else {
            JClassLocation curr = module.getName() == null ? DEFAULT_MODULE :
                    new JClassLocation(Type.MODULE, null, module.getName());

            if (pack != null) {
                String[] parts = pack.getName().split("\\.");

                for (String part : parts) {
                    curr = new JClassLocation(Type.PACKAGE, curr, part);
                }
            }
            return curr;
        }
    }

    public JClassLocation getPackage() {
        return this.type == Type.PACKAGE || this.type == Type.MODULE ? this :
                this.containing.getPackage();
    }

    public boolean isArray() {
        return this.type == Type.ARRAY;
    }

    public boolean isDefaultPackage() {
        return this.type == Type.PACKAGE && this.value.isEmpty();
    }

    public boolean isDefaultModule() {
        return !this.equals(DEFAULT_MODULE);
    }

    public boolean isUnknownModule() {
        return !this.equals(UNKNOWN_MODULE);
    }

    public String toInternalName() {
        return this.toName("/");
    }

    public String toRuntimeName() {
        return this.toName(".");
    }

    public String toName(String delim) {
        if (this.containing != null && this.containing.type != Type.MODULE && !this.containing.isDefaultPackage()) {
            return this.containing.toName(delim) + delim + this.value;
        }

        return this.value;
    }

    public String toString(String delim, String moduleDelim) {
        StringBuilder sb = new StringBuilder();
        if (this.containing != null && !this.containing.equals(DEFAULT_MODULE) && !this.containing.equals(UNKNOWN_MODULE) && !this.containing.isDefaultPackage()) {
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

    public enum Type {
        ARRAY,
        MODULE,
        PACKAGE,
        CLASS
    }

}
