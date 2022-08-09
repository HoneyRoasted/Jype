package honeyroasted.jype.system.solver.inference;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.solver.TypeConstraint;

public interface InferenceConstraint extends TypeConstraint {

    InferenceConstraint flatten();

    record Compatible(TypeConcrete left, TypeConcrete right) implements InferenceConstraint {

        @Override
        public String toString() {
            return "{" + this.left + " -> " + this.right + "}";
        }

        @Override
        public InferenceConstraint flatten() {
            return new Compatible(this.left.flatten(), this.right.flatten());
        }
    }

    record Subtype(TypeConcrete subtype, TypeConcrete parent) implements InferenceConstraint {

        @Override
        public String toString() {
            return "{" + this.subtype + " <: " + this.parent + "}";
        }

        @Override
        public InferenceConstraint flatten() {
            return new Subtype(this.subtype.flatten(), this.parent.flatten());
        }
    }

    record Contains(TypeConcrete contained, TypeConcrete container) implements InferenceConstraint {

        @Override
        public String toString() {
            return "{" + this.contained + " <= " + this.container + "}";
        }

        @Override
        public InferenceConstraint flatten() {
            return new Contains(this.contained.flatten(), this.container.flatten());
        }
    }

    record Equals(TypeConcrete left, TypeConcrete right) implements InferenceConstraint {

        @Override
        public String toString() {
            return "{" + this.left + " = " + this.right + "}";
        }

        @Override
        public InferenceConstraint flatten() {
            return new Equals(this.left.flatten(), this.right.flatten());
        }
    }

    record Throws(TypeConcrete type) implements InferenceConstraint {

        @Override
        public String toString() {
            return "{throws " + this.type + "}";
        }

        @Override
        public InferenceConstraint flatten() {
            return new Throws(this.type.flatten());
        }
    }

}
