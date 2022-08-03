package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;

import java.util.Objects;
import java.util.function.Function;

public class TypeIn extends AbstractType implements TypeConcrete {
    private TypeConcrete bound;

    public TypeIn(TypeSystem system, TypeConcrete bound) {
        super(system);
        this.bound = bound;
    }

    public TypeConcrete bound() {
        return bound;
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        TypeString bound = this.bound.toSignature(context);
        return bound.successful() ? TypeString.successful("-" + bound.value(), getClass(), TypeString.Target.SIGNATURE) : bound;
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.failure(TypeIn.class, TypeString.Target.DESCRIPTOR);
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        TypeString bound = this.bound.toSource(context);
        return bound.successful() ? TypeString.successful("? super " + bound.value(), getClass(), TypeString.Target.SOURCE) : bound;
    }

    @Override
    public TypeString toReadable(TypeString.Context context) {
        TypeString bound = this.bound.toReadable(context);
        return bound.successful() ? TypeString.successful("? super " + bound.value(), getClass(), TypeString.Target.READABLE) : bound;
    }

    @Override
    public <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(new TypeIn(this.typeSystem(), this.bound.map(mapper)));
    }

    @Override
    public TypeConcrete flatten() {
        return new TypeIn(this.typeSystem(), this.bound.flatten());
    }

    @Override
    public String toString() {
        return "? super " + this.bound;
    }

    @Override
    public boolean equalsExactly(TypeConcrete o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeIn typeIn = (TypeIn) o;

        return Objects.equals(bound, typeIn.bound);
    }

    @Override
    public int hashCodeExactly() {
        return bound != null ? bound.hashCode() : 0;
    }
}
