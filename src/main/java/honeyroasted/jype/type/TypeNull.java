package honeyroasted.jype.type;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeConstraint;
import honeyroasted.jype.system.TypeSystem;

public class TypeNull implements TypeConcrete {
    public static final TypeNull NULL = new TypeNull();

    @Override
    public TypeConstraint assignabilityTo(TypeConcrete other, TypeSystem system) {
        if (other instanceof TypePrimitive) {
            return TypeConstraint.FALSE;
        }

        return TypeConcrete.defaultTests(this, other, system, TypeConstraint.TRUE);
    }

    @Override
    public String toString() {
        return "<null>";
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && getClass() == obj.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        return TypeString.failure("TypeNull", "signature");
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.failure("TypeNull", "descriptor");
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        return TypeString.failure("TypeNull", "source");
    }
}
