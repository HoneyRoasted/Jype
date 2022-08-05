package honeyroasted.jype.type;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;

import java.util.Objects;

public class TypePrimitive extends AbstractType implements TypeConcrete {
    private Class<?> reflectionClass;
    private String descriptor;

    public TypePrimitive(TypeSystem system, Class<?> reflectionClass, String descriptor) {
        super(system);
        this.reflectionClass = reflectionClass;
        this.descriptor = descriptor;
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

}
