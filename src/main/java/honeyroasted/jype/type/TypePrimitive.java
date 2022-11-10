package honeyroasted.jype.type;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;

import java.util.Objects;

/**
 * Represents a primitive type. One of boolean, byte, short, char, int, long, float, or double. Primitive types
 * are assignable to other, wider primitive types, and may also be boxed in the relevant reference types to
 * allow assignability to other reference types.
 */
public class TypePrimitive extends AbstractType implements TypeConcrete {
    private Class<?> reflectionClass;
    private String descriptor;

    /**
     * Creates a new {@link TypePrimitive}.
     *
     * @param system          The {@link TypeSystem} this {@link TypePrimitive} is a member of
     * @param reflectionClass The primitive {@link Class} corresponding to this {@link TypePrimitive}
     * @param descriptor      The primitive descriptor for this {@link TypePrimitive}
     */
    public TypePrimitive(TypeSystem system, Class<?> reflectionClass, String descriptor) {
        super(system);
        if (!reflectionClass.isPrimitive() || reflectionClass == void.class) {
            throw new IllegalArgumentException("Invalid primitive class: " + reflectionClass);
        }
        this.reflectionClass = reflectionClass;
        this.descriptor = descriptor;
    }

    /**
     * @return The primitive {@link Class} corresponding to this {@link TypePrimitive}
     */
    public Class<?> reflectionClass() {
        return this.reflectionClass;
    }

    /**
     * @return The primitive descriptor for this {@link TypePrimitive}
     */
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

}
