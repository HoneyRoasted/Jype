package honeyroasted.jype.system;

import honeyroasted.jype.model.name.ClassNamespace;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.PrimitiveType;

public class TypeSystem {
    public final ClassType OBJECT = null;

    public final NoneType VOID = new NoneType(this, "void");
    public final NoneType NULL = new NoneType(this, "null");
    public final NoneType NONE = new NoneType(this, "none");
    public final NoneType ERROR = new NoneType(this, "error");

    public final PrimitiveType BOOLEAN = new PrimitiveType(this, ClassNamespace.of(boolean.class), ClassNamespace.of(Boolean.class));
    public final PrimitiveType BYTE = new PrimitiveType(this, ClassNamespace.of(byte.class), ClassNamespace.of(Byte.class));
    public final PrimitiveType SHORT = new PrimitiveType(this, ClassNamespace.of(short.class), ClassNamespace.of(Short.class));
    public final PrimitiveType CHAR = new PrimitiveType(this, ClassNamespace.of(char.class), ClassNamespace.of(Character.class));
    public final PrimitiveType INT = new PrimitiveType(this, ClassNamespace.of(int.class), ClassNamespace.of(Integer.class));
    public final PrimitiveType LONG = new PrimitiveType(this, ClassNamespace.of(long.class), ClassNamespace.of(Long.class));
    public final PrimitiveType FLOAT = new PrimitiveType(this, ClassNamespace.of(float.class), ClassNamespace.of(Float.class));
    public final PrimitiveType DOUBLE = new PrimitiveType(this, ClassNamespace.of(double.class), ClassNamespace.of(Double.class));

}
