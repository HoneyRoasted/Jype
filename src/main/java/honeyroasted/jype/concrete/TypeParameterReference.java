package honeyroasted.jype.concrete;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.declaration.TypeParameter;
import honeyroasted.jype.system.TypeConstraint;

import java.util.Objects;

public class TypeParameterReference implements TypeConcrete {
    private TypeParameter variable;

    public TypeParameterReference(TypeParameter variable) {
        this.variable = variable;
    }

    public TypeParameter variable() {
        return this.variable;
    }

    @Override
    public TypeConstraint assignabilityTo(TypeConcrete other) {
        return new TypeConstraint.Bound(this, other);
    }

    @Override
    public String toString() {
        return this.variable.name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeParameterReference reference = (TypeParameterReference) o;

        return Objects.equals(variable, reference.variable);
    }

    @Override
    public int hashCode() {
        return variable != null ? variable.hashCode() : 0;
    }
}
