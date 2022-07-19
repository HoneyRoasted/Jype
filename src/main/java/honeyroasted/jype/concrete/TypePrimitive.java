package honeyroasted.jype.concrete;

import java.util.Map;
import java.util.Set;

public class TypePrimitive implements TypeConcrete {
    public static final TypePrimitive BOOLEAN = new TypePrimitive(boolean.class, "Z");
    public static final TypePrimitive BYTE = new TypePrimitive(byte.class, "B");
    public static final TypePrimitive CHAR = new TypePrimitive(char.class, "C");
    public static final TypePrimitive SHORT = new TypePrimitive(short.class, "S");
    public static final TypePrimitive INT = new TypePrimitive(int.class, "I");
    public static final TypePrimitive LONG = new TypePrimitive(long.class, "J");
    public static final TypePrimitive FLOAT = new TypePrimitive(float.class, "F");
    public static final TypePrimitive DOUBLE = new TypePrimitive(double.class, "D");

    private Class<?> reflectionClass;
    private String descriptor;

    public TypePrimitive(Class<?> reflectionClass, String descriptor) {
        this.reflectionClass = reflectionClass;
        this.descriptor = descriptor;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    private static final Map<String, Set<String>> PRIM_SUPERS = Map.of(
            "Z", Set.of(),
            "B", Set.of("B", "S", "C", "I", "J", "F", "D"),
            "S", Set.of("S", "C", "I", "J", "F", "D"),
            "C", Set.of("C", "S", "I", "J", "F", "D"),
            "I", Set.of("I", "J", "F", "D"),
            "J", Set.of("J", "F", "D"),
            "F", Set.of("F", "D"),
            "D", Set.of("D")
    );

}
