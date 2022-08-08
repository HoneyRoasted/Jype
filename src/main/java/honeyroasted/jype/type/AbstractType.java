package honeyroasted.jype.type;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeSystem;

/**
 * This is a utility class that handles common code for implementations of {@link TypeConcrete}, including
 * holding a reference to a {@link TypeSystem} and redirecting {@link Object#equals(Object)} and {@link Object#hashCode()}
 * to {@link TypeConcrete#equalsExactly(TypeConcrete)} and {@link TypeConcrete#hashCodeExactly()}.
 */
public abstract class AbstractType implements TypeConcrete {
    private TypeSystem typeSystem;

    /**
     * A constructor to provide the reference to a {@link TypeSystem}.
     *
     * @param typeSystem The {@link TypeSystem} associated with this type
     */
    public AbstractType(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public TypeSystem typeSystem() {
        return this.typeSystem;
    }

    /**
     * Tests if this {@link TypeConcrete} is equal to another {@link TypeConcrete}. Note that this method considers
     * type equality, that is, if the types are equivalent they are considered equal regardless of their structure.
     * If strict structural equality is needed, use {@link TypeConcrete#equalsExactly(TypeConcrete)}.
     * <p>
     * For example, this means a {@link TypeAnd} with one component type would be considered equal to its component
     * type.
     *
     * @param obj The object to test equality against
     * @return True if this is equal to {@code obj}, false otherwise
     *
     * @see TypeConcrete#equalsExactly(TypeConcrete)
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof TypeConcrete type &&
                this.flatten().equalsExactly(type.flatten());
    }

    /**
     * Generates a hashcode based for this {@link TypeConcrete}. Note that this method considers type equality,
     * that is, two types that are equivalent will produce the same hashcode, regardless of their structure.
     * If a strict structural hash code is needed, use {@link TypeConcrete#hashCodeExactly()}.
     * <p>
     * For example, this means a {@link TypeAnd} with one component type would produce the same hash code as its
     * component type.
     *
     * @return A hash code representing this type
     *
     * @see TypeConcrete#hashCodeExactly()
     */
    @Override
    public int hashCode() {
        return this.flatten().hashCodeExactly();
    }

}
