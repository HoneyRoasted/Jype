package honeyroasted.jype;

import honeyroasted.jype.type.TypeAnd;
import honeyroasted.jype.type.TypeDeclaration;
import honeyroasted.jype.type.TypeNull;
import honeyroasted.jype.type.TypeParameter;

/**
 * This class represents the conversion of a {@link Type} to a string. There are many string representations
 * of any given {@link Type}, and certain {@link Type}s cannot be converted under certain contexts. Furthermore,
 * some compound types (such as {@link TypeAnd}) can only be converted under a certain context
 * if all their child types can also be converted under that context. Because of this, a {@link TypeString} may contain
 * a value, or it may not contain a value and report that the conversion was unsuccessful.
 */
public class TypeString {
    private String value;
    private boolean successful;

    private Class<?> type;
    private Target target;

    /**
     * Creates a new {@link TypeString}.
     *
     * @param value      The {@link String} value
     * @param successful Whether conversion to a {@link String} was successful
     * @param type       The {@link Class} of the relevant {@link Type} that is being converted
     * @param target     The target {@link String} representation
     */
    public TypeString(String value, boolean successful, Class<?> type, Target target) {
        this.value = value;
        this.successful = successful;
        this.type = type;
        this.target = target;
    }

    /**
     * Creates and returns a successful {@link TypeString}.
     *
     * @param value  The {@link String value}
     * @param type   The {@link Class} of the relevant {@link Type} that is being converted
     * @param target The target {@link String} representation
     * @return a new {@link TypeString}
     */
    public static TypeString successful(String value, Class<?> type, Target target) {
        return new TypeString(value, true, type, target);
    }

    /**
     * Creates and returns an unsuccessful {@link TypeString}.
     *
     * @param type   The {@link Class} of the relevant {@link Type} that is being converted
     * @param target The target {@link String} representation
     * @return a new {@link TypeString}
     */
    public static TypeString failure(Class<?> type, Target target) {
        return new TypeString("<unable to convert type " + target + " to " + (type == null ? "null" : type.getSimpleName()) + ">",
                false, type, target);
    }

    /**
     * @return The value of this {@link TypeString}. May hold an error message if {@link TypeString#successful()} is false.
     */
    public String value() {
        return this.value;
    }

    /**
     * @return True if conversion to a {@link String} succeeded, false otherwise.
     */
    public boolean successful() {
        return this.successful;
    }

    /**
     * @return The {@link Class} of the {@link Type} that was converted.
     */
    public Class<?> type() {
        return this.type;
    }

    /**
     * @return The target {@link String} representation.
     */
    public Target target() {
        return this.target;
    }

    /**
     * This Enum represents the context under which a {@link Type} can be converted to a {@link String}. A {@link Context#DECLARATION}
     * context refers to a class declaration or a method declaration. A {@link Context#CONCRETE} context refers to a
     * use of a type somewhere, such as a type argument or variable type. The most notable difference being that, in a
     * {@link Context#DECLARATION} context, {@link TypeParameter}s will report their bounds and {@link TypeDeclaration}s
     * will report their parameters and parents.
     *
     * @see Context#DECLARATION
     * @see Context#CONCRETE
     */
    public enum Context {
        /**
         * Represents a DECLARATION context for {@link String} conversion.
         */
        DECLARATION,
        /**
         * Represents a CONCRETE context for {@link String} conversion.
         */
        CONCRETE
    }

    /**
     * This Enum represents the different types of {@link String} representations a {@link Type} can have.
     *
     * @see Target#SOURCE
     * @see Target#DESCRIPTOR
     * @see Target#SIGNATURE
     * @see Target#READABLE
     */
    public enum Target {
        /**
         * Represents the {@link String} representation of a {@link Type} as it would be written in source code.
         * Note that some types cannot be written in source, such as {@link TypeNull}.
         */
        SOURCE,
        /**
         * Represents the {@link String} representation of a {@link Type} as it would be formatted in a Java Virtual
         * Machine Descriptor. Note that some types cannot be written as a descriptor, such as {@link TypeParameter}.
         */
        DESCRIPTOR,
        /**
         * Represents the {@link String} representation of a {@link Type} as it would be formatted in a Java Virtual
         * Machine Signature. Note that some types cannot be written as a signature, such as {@link TypeParameter.Placeholder}.
         */
        SIGNATURE,
        /**
         * Represents the {@link String} representation of a {@link Type} in a human-readable format. All {@link Type}s
         * should be able to be written in this format.
         */
        READABLE
    }

}
