package honeyroasted.jype.system.solver.inference;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.solver.TypeConstraint;
import honeyroasted.jype.type.TypeParameter;

public interface TypeBound extends TypeConstraint {

    record Equal(TypeParameter param, TypeConcrete value) implements TypeBound {
        @Override
        public String toString() {
            return "<" + this.param.toReadable(TypeString.Context.CONCRETE) + " = " + this.value.toReadable(TypeString.Context.CONCRETE) + ">";
        }
    }

    record LowerBound(TypeConcrete subtype, TypeParameter param) implements TypeBound {
        @Override
        public String toString() {
            return "<" + this.subtype.toReadable(TypeString.Context.CONCRETE) + " <: " + this.param.toReadable(TypeString.Context.CONCRETE) + ">";
        }
    }

    record UpperBound(TypeParameter subtype, TypeConcrete param) implements TypeBound {
        @Override
        public String toString() {
            return "<" + this.subtype.toReadable(TypeString.Context.CONCRETE) + " <: " + this.param.toReadable(TypeString.Context.CONCRETE) + ">";
        }
    }

    class False implements TypeBound {
        @Override
        public String toString() {
            return "false";
        }
    }

    record Capture(TypeConcrete left, TypeConcrete right) implements TypeBound {
        @Override
        public String toString() {
            return this.left.toReadable(TypeString.Context.CONCRETE) + " = capture(" + this.right.toReadable(TypeString.Context.CONCRETE) + ")";
        }
    }

    record Throws(TypeConcrete type) implements TypeBound {
        @Override
        public String toString() {
            return "throws " + this.type.toReadable(TypeString.Context.CONCRETE);
        }
    }

}
