package honeyroasted.jype.type;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.marker.TypeReference;
import honeyroasted.jype.system.TypeSystem;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class represents an array type. Given an array type A, with element E, and some other type T, the following generally holds true:
 * <ul>
 * <li>A is assignable to T if T is an array type with element Z, and E is assignable to Z</li>
 * <li>T is assignable to A if T is an array type with element Z, and Z is assignable to E</li>
 * </ul>
 * <p>
 * Note that the element of a {@link TypeArray} may be another {@link TypeArray} in the case of multidimensional arrays.
 */
public class TypeArray extends AbstractType implements TypeReference {
    private TypeConcrete element;

    /**
     * Creates a new {@link TypeArray}.
     *
     * @param system  The {@link TypeSystem} this {@link TypeArray} is a member of
     * @param element The element type of this {@link TypeArray}
     */
    public TypeArray(TypeSystem system, TypeConcrete element) {
        super(system);
        this.element = element;
    }

    /**
     * @return The direct element of this {@link TypeArray}. It may be another {@link TypeArray} for multidimensional arrays
     */
    public TypeConcrete element() {
        return this.element;
    }

    /**
     * @return The deep element of this {@link TypeArray}. Guaranteed to not be a {@link TypeArray}
     */
    public TypeConcrete deepElement() {
        return this.element instanceof TypeArray arr ? arr.deepElement() : this.element;
    }

    /**
     * @return The depth (or number of dimensions) of this array
     */
    public int depth() {
        return this.element instanceof TypeArray arr ? arr.depth() + 1 : 1;
    }

    @Override
    public TypeString toSignature(TypeString.Context context) {
        TypeString element = this.element.toSignature(context);
        return element.successful() ? TypeString.successful("[" + element.value(), getClass(), TypeString.Target.SIGNATURE) : element;
    }

    @Override
    public TypeString toDescriptor(TypeString.Context context) {
        TypeString element = this.element.toDescriptor(context);
        return element.successful() ? TypeString.successful("[" + element.value(), getClass(), TypeString.Target.DESCRIPTOR) : element;
    }

    @Override
    public TypeString toSource(TypeString.Context context) {
        TypeString element = this.element.toSource(context);
        return element.successful() ? TypeString.successful(element.value() + "[]", getClass(), TypeString.Target.SOURCE) : element;
    }

    @Override
    public TypeString toReadable(TypeString.Context context) {
        TypeString element = this.element.toReadable(context);
        return element.successful() ? TypeString.successful(element.value() + "[]", getClass(), TypeString.Target.READABLE) : element;
    }

    @Override
    public <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(new TypeArray(this.typeSystem(), this.element.map(mapper)));
    }

    @Override
    public void forEach(Consumer<TypeConcrete> consumer, Set<TypeConcrete> seen) {
        if (!seen.contains(this)) {
            seen.add(this);
            consumer.accept(this);
            this.element.forEach(consumer, seen);
        }
    }

    @Override
    public boolean isProperType() {
        return this.element.isProperType();
    }

    @Override
    public Set<TypeConcrete> circularChildren(Set<TypeConcrete> seen) {
        return this.element.circularChildren(seen, this);
    }

    @Override
    public TypeConcrete flatten() {
        return new TypeArray(this.typeSystem(), this.element.flatten());
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
