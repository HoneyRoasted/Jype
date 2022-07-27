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
}
