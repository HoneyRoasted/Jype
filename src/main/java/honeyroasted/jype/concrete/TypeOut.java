package honeyroasted.jype.concrete;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.Constraint;

import java.util.Objects;
import java.util.function.Function;

public class TypeOut implements TypeConcrete {
    private TypeConcrete bound;

    public TypeOut(TypeConcrete bound) {
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
    public <T extends Type> T map(Function<Type, Type> mapper) {
        return (T) mapper.apply(new TypeOut(this.bound.map(mapper)));
    }

    @Override
    public Constraint assignabilityTo(TypeConcrete other) {
        return this.bound.assignabilityTo(other);
    }

    @Override
    public String toString() {
        return "? extends " + this.bound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeOut typeOut = (TypeOut) o;

        return Objects.equals(bound, typeOut.bound);
    }

    @Override
    public int hashCode() {
        return bound != null ? bound.hashCode() : 0;
    }
}
