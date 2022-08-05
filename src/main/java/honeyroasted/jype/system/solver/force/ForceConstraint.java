package honeyroasted.jype.system.solver.force;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.solver.TypeConstraint;

public interface ForceConstraint extends TypeConstraint {

    record Capture(TypeConcrete left, TypeConcrete right) implements ForceConstraint {

        @Override
        public String toString() {
            return "{" + this.left.toReadable(TypeString.Context.CONCRETE).value() + "} captures {" + this.right.toReadable(TypeString.Context.CONCRETE).value() + "}";
        }
    }

}
