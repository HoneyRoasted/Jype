package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeConstraint;
import honeyroasted.jype.system.TypeSystem;

import java.util.Objects;
import java.util.function.Function;

public class TypeArray extends AbstractType implements TypeConcrete {
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
    public boolean isArray() {
        return true;
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        TypeString element = this.element.toSignature(context);
        return element.successful() ? TypeString.successful("[" + element.value()) : element;
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        TypeString element = this.element.toDescriptor(context);
        return element.successful() ? TypeString.successful("[" + element.value()) : element;
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        TypeString element = this.element.toSource(context);
        return element.successful() ? TypeString.successful(element.value() + "[]") : element;
    }

    @Override
    public <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(new TypeArray(this.element.map(mapper)));
    }

    @Override
    public TypeConcrete flatten() {
        return new TypeArray(this.element.flatten());
    }

    @Override
    public TypeConstraint assignabilityTo(TypeConcrete other, TypeSystem system) {
        if (other instanceof TypeArray arr) {
            return this.element().assignabilityTo(arr.element(), system);
        }

        return TypeConcrete.defaultTests(this, other, system,
                () -> system.OBJECT.assignabilityTo(other, system));
    }

    @Override
    public String toString() {
        return this.element + "[]";
    }

    @Override
    public boolean equalsExactly(TypeConcrete o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeArray array = (TypeArray) o;

        return Objects.equals(element, array.element);
    }

    @Override
    public int hashCodeExactly() {
        return element != null ? element.hashCode() : 0;
    }

}
