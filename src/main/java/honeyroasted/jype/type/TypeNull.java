package honeyroasted.jype.type;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;

/**
 * This class represents the null type. The only expression with this type is {@code null}, and the null type
 * is assignable to any reference type. However, no type (other than the null type itself) is assignable to the
 * null type. All instances of this class are considered equal to each other.
 */
public class TypeNull extends AbstractType implements TypeConcrete {

    /**
     * Creates a new {@link TypeNull}
     *
     * @param typeSystem The {@link TypeSystem} this {@link TypeNull} is a member of
     */
    public TypeNull(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        return TypeString.failure(TypeNull.class, TypeString.Target.SIGNATURE);
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.failure(TypeNull.class, TypeString.Target.DESCRIPTOR);
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        return TypeString.failure(TypeNull.class, TypeString.Target.SOURCE);
    }

    @Override
    public TypeString toReadable(TypeString.Context context) {
        return TypeString.successful("#null", getClass(), TypeString.Target.READABLE);
    }

    @Override
    public boolean equalsExactly(TypeConcrete obj) {
        return obj != null && getClass() == obj.getClass();
    }

    @Override
    public int hashCodeExactly() {
        return 0;
    }

}
