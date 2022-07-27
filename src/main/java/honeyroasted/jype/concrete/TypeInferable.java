package honeyroasted.jype.concrete;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.Constraint;

public class TypeInferable implements TypeConcrete {

    @Override
    public Constraint assignabilityTo(TypeConcrete other) {
        return new Constraint.InferInto(this, other);
    }

}
