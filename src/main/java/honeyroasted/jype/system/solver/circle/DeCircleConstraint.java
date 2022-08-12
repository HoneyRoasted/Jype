package honeyroasted.jype.system.solver.circle;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.solver.TypeConstraint;

public interface DeCircleConstraint extends TypeConstraint {

    record DeCircle(TypeConcrete type) implements DeCircleConstraint {

        @Override
        public String toString() {
            return "{de-circle " + this.type.toReadable(TypeString.Context.CONCRETE) + "}";
        }
    }

}
