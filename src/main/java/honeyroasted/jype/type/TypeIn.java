package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeConstraint;
import honeyroasted.jype.system.TypeSystem;

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
    public TypeString toSignature(TypeString.Context context) {
        TypeString bound = this.bound.toSignature(context);
        return bound.successful() ? TypeString.successful("-" + bound.value()) : bound;
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.failure(TypeIn.class, TypeString.Target.DESCRIPTOR);
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        TypeString bound = this.bound.toSource(context);
        return bound.successful() ? TypeString.successful("? super " + bound.value()) : bound;
    }

    @Override
    public <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(new TypeIn(this.bound.map(mapper)));
    }

    @Override
    public TypeConcrete flatten() {
        return new TypeIn(this.bound.flatten());
    }

    @Override
    public TypeConstraint assignabilityTo(TypeConcrete other, TypeSystem system) {
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
