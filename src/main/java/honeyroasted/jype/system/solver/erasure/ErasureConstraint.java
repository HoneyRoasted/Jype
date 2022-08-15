package honeyroasted.jype.system.solver.erasure;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.solver.TypeConstraint;

/**
 * This is a common interface for constraints relevant to the {@link ErasureTypeSolver}
 */
public interface ErasureConstraint extends TypeConstraint {

    /**
     * Represents a {@link TypeConstraint} that indicates that the erasure of the given type should be processed.
     *
     * @param type The {@link TypeConstraint} to perform erasure on
     */
    record Erasure(TypeConcrete type) implements ErasureConstraint {
        @Override
        public String toString() {
            return "{erasure " + this.type.toReadable(TypeString.Context.CONCRETE) + "}";
        }
    }

}
