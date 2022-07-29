package honeyroasted.jype.type;

import honeyroasted.jype.Namespace;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeConstraint;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class TypePrimitive implements TypeConcrete {
    public static final TypePrimitive BOOLEAN = new TypePrimitive(boolean.class, "Z");
    public static final TypePrimitive BYTE = new TypePrimitive(byte.class, "B");
    public static final TypePrimitive SHORT = new TypePrimitive(short.class, "S");
    public static final TypePrimitive CHAR = new TypePrimitive(char.class, "C");
    public static final TypePrimitive INT = new TypePrimitive(int.class, "I");
    public static final TypePrimitive LONG = new TypePrimitive(long.class, "J");
    public static final TypePrimitive FLOAT = new TypePrimitive(float.class, "F");
    public static final TypePrimitive DOUBLE = new TypePrimitive(double.class, "D");

    public static final Set<TypePrimitive> ALL = Set.of(BOOLEAN, BYTE, SHORT, CHAR, INT, LONG, FLOAT, DOUBLE);

    private static final Map<TypePrimitive, Namespace> PRIM_TO_BOX = Map.of(
            BOOLEAN, Namespace.of(Boolean.class),
            BYTE, Namespace.of(Byte.class),
            SHORT, Namespace.of(Short.class),
            CHAR, Namespace.of(Character.class),
            INT, Namespace.of(Integer.class),
            LONG, Namespace.of(Long.class),
            FLOAT, Namespace.of(Float.class),
            DOUBLE, Namespace.of(Double.class)
    );

    private static final Map<Namespace, TypePrimitive> BOX_TO_PRIM = reverse(PRIM_TO_BOX);

    private Class<?> reflectionClass;
    private String descriptor;

    private TypePrimitive(Class<?> reflectionClass, String descriptor) {
        this.reflectionClass = reflectionClass;
        this.descriptor = descriptor;
    }

    public Namespace box() {
        return PRIM_TO_BOX.get(this);
    }

    public static Optional<TypePrimitive> unbox(Namespace namespace) {
        return Optional.ofNullable(BOX_TO_PRIM.get(namespace));
    }

    public Class<?> reflectionClass() {
        return this.reflectionClass;
    }

    public String descriptor() {
        return this.descriptor;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    private static final Map<String, Set<String>> PRIM_SUPERS = Map.of(
            "Z", Set.of("Z"),
            "B", Set.of("B", "S", "C", "I", "J", "F", "D"),
            "S", Set.of("S", "C", "I", "J", "F", "D"),
            "C", Set.of("C", "S", "I", "J", "F", "D"),
            "I", Set.of("I", "J", "F", "D"),
            "J", Set.of("J", "F", "D"),
            "F", Set.of("F", "D"),
            "D", Set.of("D")
    );

    @Override
    public TypeConstraint assignabilityTo(TypeConcrete other) {
        if (other instanceof TypePrimitive) {
            return PRIM_SUPERS.get(this.descriptor).contains(((TypePrimitive) other).descriptor()) ?
                    TypeConstraint.TRUE : TypeConstraint.FALSE;
        } else if (other instanceof TypeClass) {
            Optional<TypePrimitive> unbox = unbox(((TypeClass) other).declaration().namespace());
            if (unbox.isPresent()) {
                return assignabilityTo(unbox.get());
            }
        }

        return TypeConcrete.defaultTests(this, other, TypeConstraint.FALSE);
    }

    @Override
    public String toString() {
        return this.reflectionClass.getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypePrimitive that = (TypePrimitive) o;

        return Objects.equals(descriptor, that.descriptor);
    }

    @Override
    public int hashCode() {
        return descriptor != null ? descriptor.hashCode() : 0;
    }

    private static <K, V> Map<K, V> reverse(Map<V, K> map) {
        Map<K, V> result = new HashMap<>();
        map.forEach((key, val) -> result.put(val, key));
        return Map.copyOf(result);
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        return toDescriptor(context);
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.successful(this.descriptor);
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        return TypeString.successful(this.reflectionClass.getSimpleName());
    }
}
