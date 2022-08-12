package honeyroasted.jype.system.solver.force;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.solver.TypeConstraint;

public interface ForceConstraint extends TypeConstraint {

    record Capture(TypeConcrete left, TypeConcrete right) implements ForceConstraint {
        @Override
        public String toString() {
            return "{" + this.left.toReadable(TypeString.Context.CONCRETE) + "} captures {" + this.right.toReadable(TypeString.Context.CONCRETE) + "}";
        }
    }

    record NonCircular(TypeConcrete type) implements ForceConstraint {
        @Override
        public String toString() {
            return "{" + this.type.toReadable(TypeString.Context.CONCRETE) + " is not circular}";
        }
    }

}
