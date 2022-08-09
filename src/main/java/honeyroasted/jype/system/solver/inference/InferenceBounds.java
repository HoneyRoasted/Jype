package honeyroasted.jype.system.solver.inference;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.solver.TypeConstraint;

public interface InferenceBounds extends TypeConstraint {

    record Equal(TypeConcrete left, TypeConcrete right) implements InferenceBounds {

        @Override
        public String toString() {
            return "{" + this.left + " = " + this.right + "}";
        }
    }

    record Subtype(TypeConcrete subtype, TypeConcrete parent) implements InferenceBounds {

        @Override
        public String toString() {
            return "{" + this.subtype + " <: " + this.parent + "}";
        }
    }

    record Captures(TypeConcrete left, TypeConcrete right) {

        @Override
        public String toString() {
            return "{" + this.left + " = capture(" + this.right + ")}";
        }
    }

    record Throws(TypeConcrete type) {

        @Override
        public String toString() {
            return "{throws " + this.type + "}";
        }
    }

}
