package honeyroasted.jype.type;

import honeyroasted.jype.Namespace;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class TypePrimitive extends AbstractType implements TypeConcrete {
    public static final TypePrimitive BOOLEAN = new TypePrimitive(boolean.class, "Z");
    public static final TypePrimitive BYTE = new TypePrimitive(byte.class, "B");
    public static final TypePrimitive SHORT = new TypePrimitive(short.class, "S");
    public static final TypePrimitive CHAR = new TypePrimitive(char.class, "C");
    public static final TypePrimitive INT = new TypePrimitive(int.class, "I");
    public static final TypePrimitive LONG = new TypePrimitive(long.class, "J");
    public static final TypePrimitive FLOAT = new TypePrimitive(float.class, "F");
    public static final TypePrimitive DOUBLE = new TypePrimitive(double.class, "D");

    public static final Set<TypePrimitive> ALL = Set.of(BOOLEAN, BYTE, SHORT, CHAR, INT, LONG, FLOAT, DOUBLE);

    private static final Map<TypePrimitive, Class<?>> PRIM_TO_BOX = Map.of(
            BOOLEAN, Boolean.class,
            BYTE, Byte.class,
            SHORT, Short.class,
            CHAR, Character.class,
            INT, Integer.class,
            LONG, Long.class,
            FLOAT, Float.class,
            DOUBLE, Double.class
    );

    private static final Map<Namespace, TypePrimitive> BOX_TO_PRIM = reverse(PRIM_TO_BOX, Namespace::of);

    private Class<?> reflectionClass;
    private String descriptor;

    private TypePrimitive(Class<?> reflectionClass, String descriptor) {
        this.reflectionClass = reflectionClass;
        this.descriptor = descriptor;
    }

    public Class<?> box() {
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
    public TypeString toSignature(TypeString.Context context) {
        return TypeString.successful(this.descriptor, getClass(), TypeString.Target.SIGNATURE);
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.successful(this.descriptor, getClass(), TypeString.Target.DESCRIPTOR);
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        return TypeString.successful(this.reflectionClass.getSimpleName(), getClass(), TypeString.Target.SOURCE);
    }

    @Override
    public TypeString toReadable(TypeString.Context context) {
        return TypeString.successful(this.reflectionClass.getSimpleName(), getClass(), TypeString.Target.READABLE);
    }

    @Override
    public String toString() {
        return this.reflectionClass.getSimpleName();
    }

    @Override
    public boolean equalsExactly(TypeConcrete o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypePrimitive that = (TypePrimitive) o;

        return Objects.equals(descriptor, that.descriptor);
    }

    @Override
    public int hashCodeExactly() {
        return descriptor != null ? descriptor.hashCode() : 0;
    }

    private static <K, V, T> Map<T, V> reverse(Map<V, K> map, Function<K, T> keyFunction) {
        Map<T, V> result = new HashMap<>();
        map.forEach((key, val) -> result.put(keyFunction.apply(val), key));
        return Map.copyOf(result);
    }

}
