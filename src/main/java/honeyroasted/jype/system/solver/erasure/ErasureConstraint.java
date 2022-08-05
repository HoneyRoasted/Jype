package honeyroasted.jype.system.solver.erasure;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.solver.TypeConstraint;

public interface ErasureConstraint extends TypeConstraint {

    record Erasure(TypeConcrete type) implements ErasureConstraint {

        @Override
        public String toString() {
            return "erasure " + type;
        }
    }

}
