package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.TypeSystem;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class represents a wildcard type of the form {@code ? super T}. Given a wildcard W of the form ? super W', and
 * some other type T, the following generally holds true:
 * <ul>
 * <li>T is assignable to W if T is assignable to W'</li>
 * <li>W is assignable to T if T is java.lang.Object</li>
 * </ul>
 * Note that wildcards of the form {@code ? extends T} are represented by {@link TypeOut}. Also, unlike most other
 * {@link Type}s, equality between references of {@link TypeIn} is based on instance equality. This is because
 * two wildcards of the form {@code ? super T} and {@code ? super T}, where T is the same type, are not considered
 * equivalent.
 */
public class TypeIn extends AbstractType implements TypeConcrete {
    private TypeConcrete bound;

    /**
     * Creates a new {@link TypeIn}.
     *
     * @param system The {@link TypeSystem} this {@link TypeIn} is a member of
     * @param bound  The bound of this {@link TypeIn}
     */
    public TypeIn(TypeSystem system, TypeConcrete bound) {
        super(system);
        this.bound = bound;
    }

    /**
     * @return The bound of this {@link TypeIn}
     */
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
    public boolean isCircular(Set<TypeConcrete> seen) {
        return seen.contains(this) || this.bound.isCircular(seen, this);
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
    public boolean equalsExactly(TypeConcrete other) {
        return this == other;
    }

    @Override
    public int hashCodeExactly() {
        return System.identityHashCode(this);
    }
}
