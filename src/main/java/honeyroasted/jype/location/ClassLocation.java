package honeyroasted.jype.location;

public record ClassLocation(Type type, ClassLocation containing, GenericDeclarationLocation declaring, String value) {
    public static final ClassLocation DEFAULT_MODULE = new ClassLocation(Type.MODULE, null, null, null);
    public static final ClassLocation UNKNOWN_MODULE = new ClassLocation(Type.MODULE, null, null, null);
    public static final ClassLocation VOID = ClassLocation.of(void.class);

    public static ClassLocation of(String internalName, GenericDeclarationLocation declaring) {
        return of(UNKNOWN_MODULE, internalName, declaring);
    }

    public static ClassLocation of(ClassLocation module, String internalName, GenericDeclarationLocation declaring) {
        String[] parts = internalName.split("/");
        if (parts.length == 1) {
            return new ClassLocation(Type.CLASS, module, declaring, parts[0]);
        } else {
            ClassLocation result = new ClassLocation(Type.PACKAGE, module, null, parts[0]);
            for (int i = 1; i < parts.length - 1; i++) {
                result = new ClassLocation(Type.PACKAGE, result, null, parts[i]);
            }
            return new ClassLocation(Type.CLASS, result, declaring, parts[parts.length - 1]);
        }
    }

    public static ClassLocation of(Class<?> cls) {
        if (cls == null) return null;

        GenericDeclarationLocation containing = cls.getEnclosingMethod() != null ? MethodLocation.of(cls.getEnclosingMethod()) :
                cls.getEnclosingConstructor() != null ? MethodLocation.of(cls.getEnclosingConstructor()) :
                        cls.getEnclosingClass() != null ? ClassNamespace.of(cls.getEnclosingClass()) : null;


        if (cls.isArray()) {
            return new ClassLocation(Type.ARRAY, of(cls.getComponentType()), containing, "[]");
        } else if (cls.isPrimitive()) {
            return new ClassLocation(Type.CLASS, DEFAULT_MODULE, containing, cls.getName());
        } else {
            String[] parts = cls.getName().split("\\.");
            return new ClassLocation(Type.CLASS, of(cls.getPackage(), cls.getModule()), containing, parts[parts.length - 1]);
        }
    }

    public static ClassLocation of(Package pack, Module module) {
        if (pack == null && module == null) {
            return null;
        } else {
            ClassLocation curr = module.getName() == null ? DEFAULT_MODULE :
                    new ClassLocation(Type.MODULE, null, null, module.getName());

            if (pack != null) {
                String[] parts = pack.getName().split("\\.");

                for (String part : parts) {
                    curr = new ClassLocation(Type.PACKAGE, curr, null, part);
                }
            }
            return curr;
        }
    }

    public ClassLocation getPackage() {
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

    enum Type {
        ARRAY,
        MODULE,
        PACKAGE,
        CLASS
    }

}
