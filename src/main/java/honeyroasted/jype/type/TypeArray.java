package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeConstraint;

import java.util.Objects;
import java.util.function.Function;

public class TypeArray implements TypeConcrete {
    private TypeConcrete element;

    public TypeArray(TypeConcrete element) {
        this.element = element;
    }

    public TypeArray(TypeConcrete element, int depth) {
        for (int i = 0; i < depth - 1; i++) {
            element = new TypeArray(element);
        }
        this.element = element;
    }

    public TypeConcrete element() {
        return this.element;
    }

    public TypeConcrete deepElement() {
        return this.element instanceof TypeArray arr ? arr.deepElement() : this.element;
    }

    public int depth() {
        return this.element instanceof TypeArray arr ? arr.depth() + 1 : 1;
    }

    @Override
    public void lock() {
        this.element.lock();
    }

    @Override
    public <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(new TypeArray(this.element.map(mapper)));
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public String toString() {
        return this.element + "[]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeArray array = (TypeArray) o;

        return Objects.equals(element, array.element);
    }

    @Override
    public int hashCode() {
        return element != null ? element.hashCode() : 0;
    }

    @Override
    public TypeConstraint assignabilityTo(TypeConcrete other) {
        if (other instanceof TypeArray arr) {
            return this.element().assignabilityTo(arr.element());
        } else if (other instanceof TypeClass otherClass) {
            //TODO some checks
        }

        return TypeConcrete.defaultTests(this, other, TypeConstraint.FALSE);
    }
}