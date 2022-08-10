package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class represents a wildcard type of the form {@code ? extends T}. Given a wildcard W of the form ? extends W',
 * and some other type T, the following generally holds true:
 * <ul>
 * <li>T is never assignable to W</li>
 * <li>W is assignable to T if W' is assignable to T</li>
 * </ul>
 * Note that the wildcard type of the form {@code ?} is equivalent to {@code ? extends Object}, and that
 * wildcards of the form {@code ? super T} are represented by {@link TypeIn}.
 */
public class TypeOut extends AbstractType implements TypeConcrete {
    private TypeConcrete bound;

    public TypeOut(TypeSystem system, TypeConcrete bound) {
        super(system);
        this.bound = bound;
    }

    public TypeConcrete bound() {
        return bound;
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        if (this.bound.equals(this.typeSystem().OBJECT)) {
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
        if (this.bound.equals(this.typeSystem().OBJECT)) {
            return TypeString.successful("?", getClass(), TypeString.Target.SOURCE);
        } else {
            TypeString bound = this.bound.toSource(context);
            return bound.successful() ? TypeString.successful("? extends " + bound.value(), getClass(), TypeString.Target.SOURCE) : bound;
        }
    }

    @Override
    public TypeString toReadable(TypeString.Context context) {
        if (this.bound.equals(this.typeSystem().OBJECT)) {
            return TypeString.successful("?", getClass(), TypeString.Target.SOURCE);
        } else {
            TypeString bound = this.bound.toReadable(context);
            return bound.successful() ? TypeString.successful("? extends " + bound.value(), getClass(), TypeString.Target.SOURCE) : bound;
        }
    }

    @Override
    public <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(new TypeOut(this.typeSystem(), this.bound.map(mapper)));
    }

    @Override
    public void forEach(Consumer<TypeConcrete> consumer, Set<TypeConcrete> seen) {
        if (!seen.contains(this)) {
            seen.add(this);
            consumer.accept(this);
            this.bound.forEach(consumer, seen);
        }
    }

    @Override
    public boolean isProperType() {
        return this.bound.isProperType();
    }

    @Override
    public boolean isWildcard() {
        return true;
    }

    @Override
    public TypeConcrete flatten() {
        return new TypeOut(this.typeSystem(), this.bound.flatten());
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
