package honeyroasted.jype.system.solver;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.type.TypePrimitive;

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
    /**
     * An instance of {@link False}.
     */
    TypeConstraint FALSE = new False();

    /**
     * An instance of {@link True}.
     */
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
     * Helper method to create a {@link TypeConstraint.Equal}.
     *
     * @param left  A {@link TypeConcrete} to check equality of
     * @param right A {@link TypeConcrete} to check equality of
     * @return A new {@link TypeConstraint.Equal}
     * @see TypeConstraint.Equal
     */
    static Equal equal(TypeConcrete left, TypeConcrete right) {
        return new Equal(left, right);
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
    }

    /**
     * Helper method to create a {@link TypeConstraint.Bound}.
     *
     * @param subtype The subtype to check assignability from
     * @param parent  The parent to check assignability to
     * @return A new {@link TypeConstraint.Bound}
     * @see TypeConstraint.Bound
     */
    static Bound bound(TypeConcrete subtype, TypeConcrete parent) {
        return new Bound(subtype, parent);
    }

    record Boxes(TypePrimitive prim, TypeConcrete box) implements TypeConstraint {

        @Override
        public String toString() {
            return "{" + this.box.toReadable(TypeString.Context.CONCRETE) + "} boxes {" + this.prim.toReadable(TypeString.Context.CONCRETE) + "}";
        }
    }

    static Boxes boxes(TypePrimitive prim, TypeConcrete box) {
        return new Boxes(prim, box);
    }

}
