package honeyroasted.jype.system.solver;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.type.TypeParameter;

/**
 * This interface represents some sort of constraint over {@link Type}s, for use in {@link TypeSolver}s. It is very
 * general and each {@link TypeSolver} may define their own constraints and/or use the default ones. A {@link TypeSolver}
 * is not required to accept every implementation of {@link TypeConstraint}, but rather only the ones relevant
 * to whatever computation it is performing.
 *
 * @see TypeConstraint.False
 * @see TypeConstraint.True
 * @see TypeConstraint.Equal
 * @see TypeConstraint.Bound
 */
public interface TypeConstraint {
    TypeConstraint FALSE = new False();
    TypeConstraint TRUE = new True();

    /**
     * Represents a {@link TypeConstraint} which always evaluates to false
     */
    class False implements TypeConstraint {
        @Override
        public String toString() {
            return "FALSE";
        }
    }

    /**
     * Represents a {@link TypeConstraint} which always evaluates to true
     */
    class True implements TypeConstraint {
        @Override
        public String toString() {
            return "TRUE";
        }
    }

    /**
     * Represents a {@link TypeConstraint} that requires {@code left} to be equal to {@code right}.
     *
     * @param left  A {@link TypeConcrete} to check equality of
     * @param right A {@link TypeConcrete} to check equality of
     */
    record Equal(TypeConcrete left, TypeConcrete right) implements TypeConstraint {
        @Override
        public String toString() {
            return "{" + this.left.toReadable(TypeString.Context.CONCRETE) + "} is equal to {" + this.right.toReadable(TypeString.Context.CONCRETE) + "}";
        }
    }

    /**
     * Represents a {@link TypeConcrete} that requires {@code subtype} is assignable to {@code parent}.
     *
     * @param subtype The subtype to check assignability from
     * @param parent  The parent to check assignability to
     */
    record Bound(TypeConcrete subtype, TypeConcrete parent) implements TypeConstraint {

        @Override
        public String toString() {
            return "{" + this.subtype.toReadable(TypeString.Context.CONCRETE) + "} is assignable to {" + this.parent.toReadable(TypeString.Context.CONCRETE) + "}";
        }

        public Kind kind() {
            if (this.subtype instanceof TypeParameter && this.parent instanceof TypeParameter) {
                return Kind.VAR_TO_VAR;
            } else if (this.subtype instanceof TypeParameter) {
                return Kind.VAR_TO_BOUND;
            } else if (this.parent instanceof TypeParameter) {
                return Kind.BOUND_TO_VAR;
            } else {
                return Kind.BOUND_TO_BOUND;
            }
        }

        public enum Kind {
            VAR_TO_BOUND,
            BOUND_TO_VAR,
            VAR_TO_VAR,
            BOUND_TO_BOUND
        }

    }

}
