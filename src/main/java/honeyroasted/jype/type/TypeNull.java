package honeyroasted.jype.type;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeConstraint;

public class TypeNull implements TypeConcrete {
    public static final TypeNull NULL = new TypeNull();

    @Override
    public TypeConstraint assignabilityTo(TypeConcrete other) {
        if (other instanceof TypePrimitive) {
            return TypeConstraint.FALSE;
        }

        return TypeConcrete.defaultTests(this, other, TypeConstraint.TRUE);
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
}
