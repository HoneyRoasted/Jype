package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;

import java.util.Objects;
import java.util.function.Function;

public class TypeOut extends AbstractType implements TypeConcrete {
    private TypeConcrete bound;

    public TypeOut(TypeConcrete bound) {
        this.bound = bound;
    }

    public TypeConcrete bound() {
        return bound;
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        if (this.bound.equals(TypeSystem.GLOBAL.OBJECT)) {
            return TypeString.successful("*", getClass(), TypeString.Target.SIGNATURE);
        } else {
            TypeString bound = this.bound.toSignature(context);
            return bound.successful() ? TypeString.successful("+" + bound.value(), getClass(), TypeString.Target.SIGNATURE) : bound;
        }
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        return TypeString.failure(TypeOut.class, TypeString.Target.DESCRIPTOR);
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        if (this.bound.equals(TypeSystem.GLOBAL.OBJECT)) {
            return TypeString.successful("?", getClass(), TypeString.Target.SOURCE);
        } else {
            TypeString bound = this.bound.toSource(context);
            return bound.successful() ? TypeString.successful("? extends " + bound.value(), getClass(), TypeString.Target.SOURCE) : bound;
        }
    }

    @Override
    public TypeString toReadable(TypeString.Context context) {
        if (this.bound.equals(TypeSystem.GLOBAL.OBJECT)) {
            return TypeString.successful("?", getClass(), TypeString.Target.SOURCE);
        } else {
            TypeString bound = this.bound.toReadable(context);
            return bound.successful() ? TypeString.successful("? extends " + bound.value(), getClass(), TypeString.Target.SOURCE) : bound;
        }
    }

    @Override
    public <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(new TypeOut(this.bound.map(mapper)));
    }

    @Override
    public TypeConcrete flatten() {
        return new TypeOut(this.bound.flatten());
    }

    @Override
    public String toString() {
        return "? extends " + this.bound;
    }

    @Override
    public boolean equalsExactly(TypeConcrete o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeOut typeOut = (TypeOut) o;

        return Objects.equals(bound, typeOut.bound);
    }

    @Override
    public int hashCodeExactly() {
        return bound != null ? bound.hashCode() : 0;
    }
}
