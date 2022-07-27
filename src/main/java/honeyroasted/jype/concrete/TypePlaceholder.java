package honeyroasted.jype.concrete;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.Constraint;

public class TypePlaceholder implements TypeConcrete {
    @Override
    public Constraint assignabilityTo(TypeConcrete other) {
        return new Constraint.InferTo(this, other);
    }

    @Override
    public String toString() {
        return "<typevar #" + identity() + ">";
    }

    public String identity() {
        return Integer.toHexString(System.identityHashCode(this));
    }
}
