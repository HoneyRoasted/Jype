package honeyroasted.jype.concrete;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.declaration.TypeParameter;
import honeyroasted.jype.system.Constraint;

public class TypeParameterReference implements TypeConcrete {
    private TypeParameter variable;

    public TypeParameterReference(TypeParameter variable) {
        this.variable = variable;
    }

    public TypeParameter variable() {
        return this.variable;
    }

    @Override
    public Constraint assignabilityTo(TypeConcrete other) {
        return new Constraint.Bounded(this, other);
    }

}
