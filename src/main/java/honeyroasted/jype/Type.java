package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.TypeAnd;

/**
 * This interface represents any type in the Java type system.
 */
public interface Type {

    /**
     * Generates the {@link TypeString} of this {@link Type}'s Java Virtual Machine signature under the given
     * {@link TypeString.Context}. Some types may not have a valid signature under the given {@link TypeString.Context},
     * and will return an unsuccessful {@link TypeString}.
     *
     * @param context The context under which this {@link Type} is being converted to a {@link String}
     * @return The {@link TypeString} representation of this {@link Type}'s signature
     * @see TypeString.Context
     */
    TypeString toSignature(TypeString.Context context);

    /**
     * Generates the {@link TypeString} of this {@link Type}'s Java Virtual Machine descriptor under the given
     * {@link TypeString.Context}. Some types may not have a valid descriptor under the given {@link TypeString.Context},
     * and will return an unsuccessful {@link TypeString}.
     *
     * @param context The context under which this {@link Type} is being converted to a {@link String}
     * @return The {@link TypeString} representation of this {@link Type}'s descriptor
     * @see TypeString.Context
     */
    TypeString toDescriptor(TypeString.Context context);

    /**
     * Generates the {@link TypeString} of this {@link Type}'s source representation under the given
     * {@link TypeString.Context}. Some types may not have a valid source representation under the given
     * {@link TypeString.Context}, and will return an unsuccessful {@link TypeString}.
     *
     * @param context The context under which this {@link Type} is being converted to a {@link String}
     * @return The {@link TypeString} representation of this {@link Type}'s source
     * @see TypeString.Context
     */
    TypeString toSource(TypeString.Context context);

    /**
     * Generates the {@link TypeString} of this {@link Type}'s human-readable representation under the given
     * {@link TypeString.Context}. All types should have a human-readable representation, and the returned
     * {@link TypeString} should always be successful.
     *
     * @param context The context under which this {@link Type} is being converted to a {@link String}
     * @return The {@link TypeString} representation of this {@link Type}'s source
     * @see TypeString.Context
     */
    TypeString toReadable(TypeString.Context context);

    /**
     * @return The {@link TypeSystem} associated with this {@link Type}
     */
    TypeSystem typeSystem();

    /**
     * Locks this {@link Type}, making it immutable. Note that this does not necessarily lock the child {@link Type}s
     * of compound types such as {@link TypeAnd}. Since {@link Type} hierarchies can contain circular references, it
     * may be necessary to instantiate {@link Type}s, build their values, and then lock them to maintain immutability.
     */
    default void lock() {
    }

    default String kind() {
        return Namespace.of(this.getClass()).simpleName().replace('$', '.');
    }

}
