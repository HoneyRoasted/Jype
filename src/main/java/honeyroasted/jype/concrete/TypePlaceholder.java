package honeyroasted.jype.concrete;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeConstraint;

public class TypePlaceholder implements TypeConcrete {
    @Override
    public TypeConstraint assignabilityTo(TypeConcrete other) {
        return new TypeConstraint.Bound(this, other);
    }

    @Override
    public String toString() {
        return "<typevar #" + identity() + ">";
    }

    public String identity() {
        return Integer.toHexString(System.identityHashCode(this));
    }

}
