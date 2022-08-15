package honeyroasted.jype;

import honeyroasted.jype.type.TypeAnd;
import honeyroasted.jype.type.TypeDeclaration;
import honeyroasted.jype.type.TypeIn;
import honeyroasted.jype.type.TypeOut;
import honeyroasted.jype.type.TypeParameter;
import honeyroasted.jype.type.TypeParameterized;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This interface represents a {@link Type} in the Java type system which is not a declaration. Declarations include
 * class declarations and method declarations, whereas {@link TypeConcrete}s can be used in various places, such as
 * type arguments, method parameter types, field types, variable types, and more.
 */
public interface TypeConcrete extends Type {

    /**
     * Applies a mapper function over this {@link Type} and its child {@link Type}s, if any. This method
     * does not walk the <i>entire</i> {@link Type} tree. Specifically, {@link TypeParameterized} will
     * not apply the mapper to its corresponding {@link TypeDeclaration} and {@link TypeParameter}
     * will not apply the mapper to its corresponding bound. This is to prevent infinite recursion, and to
     * keep instances of {@link TypeParameter} the same.
     *
     * @param mapper The mapper function to apply
     * @param <T>    The return {@link Type}. This may cause a {@link ClassCastException} if the mapper does not produce
     *               a type of {@code T} for the highest level {@link Type}
     * @return A (possibly new) {@link Type} constructed by mapping this {@link Type} and its child {@link Type}s
     */
    default <T extends Type> T map(Function<TypeConcrete, TypeConcrete> mapper) {
        return (T) mapper.apply(this);
    }

    /**
     * Applies a consumer function over this {@link Type} and its child {@link Type}s, if any. This method does not
     * walk the <i>entire</i> {@link Type} tree. Specifically, {@link TypeParameterized} will not apply the consumer to its
     * corresponding {@link TypeDeclaration}. However, this method will apply the consumer to each {@link TypeParameter}
     * and its bounds (unlike {@link TypeConcrete#map(Function)}). To prevent recursion if the bounds are circular,
     * a {@link Set} containing the visited types is passed to this function and each child invocation of the function,
     * ensuring any instance of a type is visited only once.
     * <p>
     * Note that this method is primarily called within the {@link TypeConcrete#forEach(Consumer, Set)} implementation
     * to pass the visited type set to children {@link Type}s. For general purpose use, {@link TypeConcrete#forEach(Consumer)}
     * should be used.
     *
     * @param consumer The action to apply to each {@link TypeConcrete}
     * @param seen     The set of types already visited
     * @see TypeConcrete#forEach(Consumer)
     */
    default void forEach(Consumer<TypeConcrete> consumer, Set<TypeConcrete> seen) {
        if (!seen.contains(this)) {
            seen.add(this);
            consumer.accept(this);
        }
    }

    /**
     * Applies a consumer function over this {@link Type} and its child {@link Type}s, if any. This method does not
     * walk the <i>entire</i> {@link Type} tree. Specifically, {@link TypeParameterized} will not apply the consumer to its
     * corresponding {@link TypeDeclaration}. However, this method will apply the consumer to each {@link TypeParameter}
     * and its bounds (unlike {@link TypeConcrete#map(Function)}).
     * <p>
     * This is equivalent to calling {@code forEach(consumer, new HashSet<>())}.
     *
     * @param consumer The action to apply to each {@link TypeConcrete}
     * @see TypeConcrete#forEach(Consumer, Set)
     */
    default void forEach(Consumer<TypeConcrete> consumer) {
        forEach(consumer, new HashSet<>());
    }

    /**
     * @return True if this is a proper type, that is, it contains no {@link TypeParameter}s, false otherwise.
     */
    default boolean isProperType() {
        return true;
    }

    /**
     * @return True if this is not a circular type, that is it contains no {@link TypeParameter}s, {@link TypeOut}s,
     * or {@link TypeIn}s which eventually refer to themselves. For example, given {@code <T extends List<B>, B extends List<T>>},
     * both {@code T} and {@code B} are considered circular
     */
    default boolean isCircular() {
        return this.circularChildren().isEmpty();
    }

    /**
     * @return The {@link Set} of {@link TypeConcrete}s that are children of this {@link TypeConcrete} and are circular.
     * Note that this {@link TypeConcrete} is considered circular if any of its children are circular, but this method
     * specifically looks for the {@link TypeParameter}s, {@link TypeOut}s, and {@link TypeIn}s which cause the
     * circular reference
     */
    default Set<TypeConcrete> circularChildren() {
        return this.circularChildren(new HashSet<>());
    }


    /**
     * This is a utility method for the implementation of {@link TypeConcrete#circularChildren(Set)}.
     *
     * @param seen The {@link Set} of {@link TypeConcrete}s already seen
     * @param parent The {@link TypeConcrete} calling this method
     * @return The {@link TypeConcrete#circularChildren()} of this {@link TypeConcrete}
     */
    default Set<TypeConcrete> circularChildren(Set<TypeConcrete> seen, TypeConcrete parent) {
        Set<TypeConcrete> newSeen = new HashSet<>(seen);
        newSeen.add(parent);
        return this.circularChildren(newSeen);
    }

    /**
     * This is the implementation method for {@link TypeConcrete#circularChildren()}.
     *
     * @param seen The {@link Set} of {@link TypeConcrete}s already seen
     * @return The {@link TypeConcrete#circularChildren()} of this {@link TypeConcrete}
     */
    default Set<TypeConcrete> circularChildren(Set<TypeConcrete> seen) {
        if (seen.contains(this)) {
            return Set.of(this);
        } else {
            return Set.of();
        }
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
