package honeyroasted.jype.metadata.location;

import org.glavo.classfile.ClassModel;
import org.glavo.classfile.constantpool.ClassEntry;

import java.lang.constant.ClassDesc;
import java.util.ArrayList;
import java.util.List;

public record JClassLocation(Type type, JClassLocation containing, String value) {
    public static final JClassLocation DEFAULT_PACKAGE = new JClassLocation(Type.PACKAGE, null, "");
    public static final JClassLocation VOID = JClassLocation.of(void.class);

    public static JClassLocation of(ClassModel model) {
        return of(model.thisClass().asInternalName());
    }

    public static JClassLocation of(ClassEntry entry) {
        return of(entry.asInternalName());
    }

    public static JClassLocation of(ClassDesc desc) {
        return ofDescriptor(desc.descriptorString());
    }

    public static JClassLocation ofDescriptor(String desc) {
        if (desc.startsWith("[")) {
            int depth = 0;
            while (desc.startsWith("[")) {
                depth++;
                desc = desc.substring(1);
            }

            JClassLocation curr = ofDescriptor(desc);
            for (int i = 0; i < depth; i++) {
                curr = new JClassLocation(Type.ARRAY, curr, "[]");
            }

            return curr;
        } else if (desc.isEmpty()) {
            return DEFAULT_PACKAGE;
        } else if (desc.length() == 1) {
            return switch (desc) {
                case "Z" -> of(boolean.class);
                case "B" -> of(byte.class);
                case "S" -> of(short.class);
                case "C" -> of(char.class);
                case "I" -> of(int.class);
                case "J" -> of(long.class);
                case "F" -> of(float.class);
                case "D" -> of(double.class);
                case "V" -> VOID;
                default -> DEFAULT_PACKAGE;
            };
        } else {
            //cut out L and ;
            desc = desc.substring(1, desc.length() - 1);
            return of(desc);
        }
    }

    public static JClassLocation of(String internalName) {
        String[] parts = internalName.split("/");
        if (parts.length == 1) {
            return new JClassLocation(Type.CLASS, DEFAULT_PACKAGE, parts[0]);
        } else {
            JClassLocation result = null;
            for (int i = 0; i < parts.length - 1; i++) {
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
            return new JClassLocation(Type.CLASS, null, cls.getName());
        } else {
            String[] parts = cls.getName().split("\\.");
            return new JClassLocation(Type.CLASS, of(cls.getPackage()), parts[parts.length - 1]);
        }
    }

    public static JClassLocation of(Package pack) {
            if (pack != null) {
                JClassLocation curr = null;
                String[] parts = pack.getName().split("\\.");

                for (String part : parts) {
                    curr = new JClassLocation(Type.PACKAGE, curr, part);
                }
                return curr;
            } else {
                return DEFAULT_PACKAGE;
            }
    }

    public JClassName toName() {
        JClassName containing = this.containing == null ? null : this.containing.toName();
        return switch (this.type) {
            case ARRAY -> new JClassName(JClassName.Type.CLASS, JClassName.SubType.ARRAY, containing, this.value);
            case PACKAGE -> new JClassName(JClassName.Type.PACKAGE, JClassName.SubType.NONE, containing, this.value);
            case CLASS -> new JClassName(JClassName.Type.CLASS, JClassName.SubType.NONE, containing, this.value);
        };
    }

    public JClassLocation getPackage() {
        return this.type == Type.PACKAGE ? this :
                this.containing == null ? DEFAULT_PACKAGE :
                this.containing.getPackage();
    }

    public boolean isArray() {
        return this.type == Type.ARRAY;
    }

    public boolean isDefaultPackage() {
        return this.type == Type.PACKAGE && this.value.isEmpty();
    }

    public String simpleName() {
        if (this.type == Type.PACKAGE) {
            return this.toRuntimeName();
        }

        if (this.containing != null && this.containing.type != Type.PACKAGE) {
            return this.containing.simpleName() + "." + this.value;
        }

        return this.value;
    }

    public String toInternalName() {
        return this.toName("/");
    }

    public String toRuntimeName() {
        return this.toName(".");
    }

    public String toName(String delim) {
        if (this.containing != null && !this.containing.isDefaultPackage()) {
            return this.containing.toName(delim) + delim + this.value;
        }

        return this.value;
    }

    public String toString(String delim) {
        StringBuilder sb = new StringBuilder();
        if (this.containing != null && !this.containing.isDefaultPackage()) {
            sb.append(this.containing);

            if (!this.isArray()) {
                sb.append(delim);
            }
        }

        if (this.value != null) {
            sb.append(this.value);
        }

        return sb.toString();
    }

    public String toString() {
        return this.toString(".");
    }

    public String[] toArray() {
        List<String> result = new ArrayList<>();
        toArray(result);
        return result.toArray(String[]::new);
    }

    private void toArray(List<String> str) {
        if (this.containing != null && !this.containing.isDefaultPackage()) {
            this.containing.toArray(str);
        }

        str.add(this.value);
    }

    public enum Type {
        ARRAY,
        PACKAGE,
        CLASS
    }

}
