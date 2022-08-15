package honeyroasted.jype.system.solver.force;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.solver.TypeConstraint;

/**
 * This is a common interface for constraints relevant to the {@link ForceResolveTypeSolver}
 */
public interface ForceConstraint extends TypeConstraint {

    /**
     * Represents a {@link TypeConstraint} that requires {@code left} to capture {@code right}, per the rules of
     * capture conversion. Note that this {@link TypeConstraint} is purely information. It cannot be accepted by
     * {@link ForceResolveTypeSolver}, but it may be produced as a reason for failure.
     *
     * @param left The {@link TypeConcrete} that should capture {@code right}
     * @param right The {@link TypeConcrete} that should be capture by {@code left}
     */
    record Capture(TypeConcrete left, TypeConcrete right) implements ForceConstraint {
        @Override
        public String toString() {
            return "{" + this.left.toReadable(TypeString.Context.CONCRETE) + "} captures {" + this.right.toReadable(TypeString.Context.CONCRETE) + "}";
        }
    }

}
