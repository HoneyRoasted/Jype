package honeyroasted.jype.type;

import honeyroasted.jype.model.name.ClassNamespace;

import java.util.List;

public class PrimitiveType implements Type {
    public static final PrimitiveType BOOLEAN = new PrimitiveType(boolean.class, Boolean.class);
    public static final PrimitiveType BYTE = new PrimitiveType(byte.class, Byte.class);
    public static final PrimitiveType CHAR = new PrimitiveType(char.class, Character.class);
    public static final PrimitiveType SHORT = new PrimitiveType(short.class, Short.class);
    public static final PrimitiveType INT = new PrimitiveType(int.class, Integer.class);
    public static final PrimitiveType LONG = new PrimitiveType(long.class, Long.class);
    public static final PrimitiveType FLOAT = new PrimitiveType(float.class, Float.class);
    public static final PrimitiveType DOUBLE = new PrimitiveType(double.class, Double.class);
    public static final List<PrimitiveType> ALL = List.of(BOOLEAN, BYTE, CHAR, SHORT, INT, LONG, FLOAT, DOUBLE);

    private ClassNamespace namespace;
    private ClassNamespace boxNamespace;
    private Class<?> cls;
    private Class<?> boxCls;

    private PrimitiveType(Class<?> cls, Class<?> boxCls) {
        this.namespace = ClassNamespace.of(cls);
        this.boxNamespace = ClassNamespace.of(boxCls);

        this.cls = cls;
        this.boxCls = boxCls;
    }

    public ClassNamespace namespace() {
        return namespace;
    }

    public ClassNamespace boxNamespace() {
        return boxNamespace;
    }

    public Class<?> cls() {
        return cls;
    }

    public Class<?> boxCls() {
        return boxCls;
    }
}
