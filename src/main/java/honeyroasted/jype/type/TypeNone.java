package honeyroasted.jype.type;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.marker.TypePsuedo;
import honeyroasted.jype.system.TypeSystem;

/**
 * This class represents a type that does not exist, or is absent. All instances of this class are equivalent to {@code void}.
 * {@code void} is not assignable to any type, and there are no types assignable to {@code void}. Equality between
 * references to this class is determined by instance equality.
 */
public class TypeNone extends AbstractType implements TypePsuedo {
    private String name;

    /**
     * Creates a new {@link TypeNone}
     *
     * @param system The {@link TypeSystem} this {@link TypeNone} is a member of
     * @param name   The name of this {@link TypeNone}
     */
    public TypeNone(TypeSystem system, String name) {
        super(system);
        this.name = name;
    }

    /**
     * @return The name of this {@link TypeNone}
     */
    public String name() {
        return this.name;
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        return TypeString.successful("V", getClass(), TypeString.Target.SIGNATURE);
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.successful("V", getClass(), TypeString.Target.DESCRIPTOR);
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        return TypeString.successful("void", getClass(), TypeString.Target.SOURCE);
    }

    @Override
    public TypeString toReadable(TypeString.Context context) {
        return TypeString.successful("#" + this.name, getClass(), TypeString.Target.READABLE);
    }

    @Override
    public boolean equalsExactly(TypeConcrete other) {
        return this == other;
    }

    @Override
    public int hashCodeExactly() {
        return System.identityHashCode(this);
    }

}
