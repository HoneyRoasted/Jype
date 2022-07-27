package honeyroasted.jype.concrete;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.Constraint;

public class TypeNull implements TypeConcrete {
    public static final TypeNull NULL = new TypeNull();

    @Override
    public Constraint assignabilityTo(TypeConcrete other) {
        if (other instanceof TypePrimitive) {
            return Constraint.FALSE;
        }

        return TypeConcrete.defaultTests(this, other, Constraint.TRUE);
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
