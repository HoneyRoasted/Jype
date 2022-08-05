package honeyroasted.jype;

import honeyroasted.jype.type.TypeAnd;
import honeyroasted.jype.type.TypeClass;
import honeyroasted.jype.type.TypeDeclaration;
import honeyroasted.jype.type.TypeParameter;

import java.util.function.Function;

/**
 * This interface represents a {@link Type} in the Java type system which is not a declaration. Declarations include
 * class declarations and method declarations, whereas {@link TypeConcrete}s can be used in various places, such as
 * type arguments, method parameter types, field types, variable types, and more.
 */
public interface TypeConcrete extends Type {

    /**
     * Applies a mapper function over this {@link Type} and its child {@link Type}s, if any. Notably this method
     * does not walk the <i>entire</i> {@link Type} tree. Specifically, {@link TypeClass} will
     * not apply the mapper to its corresponding {@link TypeDeclaration} and {@link TypeParameter}
     * will not apply the mapper to its corresponding bound. This is to prevent infinite recursion, and to
     * keep instances of {@link TypeParameter} the same.
     *
     * @param mapper The mapper function to apply
     * @return A (possibly new) {@link Type} constructed by mapping this {@link Type} and its child {@link Type}s
     *
     * @param <T> The return {@link Type}. This may cause a {@link ClassCastException} if the mapper does not produce
     *           a type of {@code T} for the highest level {@link Type}
     */
    default <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(this);
    }

    /**
     * Flattens this {@link TypeConcrete}, simplifying its structure. For example, any {@link TypeAnd}
     * with a single element will be reduced to just that element. This may be necessary for checking
     * type equality.
     *
     * @return A new simplified {@link TypeConcrete}, or this
     */
    default TypeConcrete flatten() {
        return this;
    }

    /**
     * Checks if this {@link TypeConcrete} is exactly equal to another {@link TypeConcrete}. This requires
     * the same type structure, whereas the normal {@link Object#equals(Object)} method on {@link TypeConcrete}s
     * checks for type equality (e.g., a {@link TypeAnd} with one element is considered equal to any type which is
     * equal to that element).
     *
     * @param other The other {@link TypeConcrete} to check equality against
     * @return True if the {@code this} is exactly structurally equal to {@code other}
     */
    boolean equalsExactly(TypeConcrete other);

    /**
     * Generates a hash code from the exact type structure represented by this {@link TypeConcrete}, wherease the normal
     * {@link Object#hashCode()} on {@link TypeConcrete}s is compliant with type equality (e.g. a {@link TypeAnd} with
     * one element will have the same hash code as any type which is equal to that element).
     *
     * @return The hash code of this {@link TypeConcrete}'s exact structure
     */
    int hashCodeExactly();

}
