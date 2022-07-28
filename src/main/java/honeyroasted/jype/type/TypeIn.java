package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeConstraint;

import java.util.Objects;
import java.util.function.Function;

public class TypeIn implements TypeConcrete {
    private TypeConcrete bound;

    public TypeIn(TypeConcrete bound) {
        this.bound = bound;
    }

    public TypeConcrete bound() {
        return bound;
    }

    @Override
    public void lock() {
        this.bound.lock();
    }

    @Override
    public <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(new TypeIn(this.bound.map(mapper)));
    }

    @Override
    public TypeConstraint assignabilityTo(TypeConcrete other) {
        return TypeConstraint.FALSE;
    }

    @Override
    public String toString() {
        return "? super " + this.bound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeIn typeIn = (TypeIn) o;

        return Objects.equals(bound, typeIn.bound);
    }

    @Override
    public int hashCode() {
        return bound != null ? bound.hashCode() : 0;
    }
}
