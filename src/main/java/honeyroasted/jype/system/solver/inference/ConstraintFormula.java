package honeyroasted.jype.system.solver.inference;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;
import honeyroasted.jype.system.solver.TypeConstraint;
import honeyroasted.jype.system.solver.inference.model.ExpressionModel;
import honeyroasted.jype.system.solver.inference.model.LambdaExpressionModel;
import honeyroasted.jype.system.solver.inference.model.MethodReferenceModel;

public interface ConstraintFormula extends TypeConstraint {

    record TypeCompatible(TypeConcrete subtype, TypeConcrete parent) implements ConstraintFormula {
        @Override
        public String toString() {
            return "< " + this.subtype.toReadable(TypeString.Context.CONCRETE) + " --> " + this.parent.toReadable(TypeString.Context.CONCRETE) + " >";
        }
    }

    record ExpressionCompatible(ExpressionModel expression, TypeConcrete parent) implements ConstraintFormula {
        @Override
        public String toString() {
            return "<" + this.expression + " --> " + this.parent.toReadable(TypeString.Context.CONCRETE) + ">";
        }
    }

    record Subtype(TypeConcrete subtype, TypeConcrete parent) implements ConstraintFormula {
        @Override
        public String toString() {
            return "< " + this.subtype.toReadable(TypeString.Context.CONCRETE) + " <: " + this.parent.toReadable(TypeString.Context.CONCRETE) + " >";
        }
    }

    record Contained(TypeConcrete subtype, TypeConcrete parent) implements ConstraintFormula {
        @Override
        public String toString() {
            return "< " + this.subtype.toReadable(TypeString.Context.CONCRETE) + " <= " + this.parent.toReadable(TypeString.Context.CONCRETE) + " >";
        }
    }

    record Equal(TypeConcrete left, TypeConcrete right) implements ConstraintFormula {
        @Override
        public String toString() {
            return "< " + this.left.toReadable(TypeString.Context.CONCRETE) + " = " + this.right.toReadable(TypeString.Context.CONCRETE) + " >";
        }
    }

    record LambdaThrows(LambdaExpressionModel lambda, TypeConcrete type) implements ConstraintFormula {
        @Override
        public String toString() {
            return "<" + this.lambda.toString() + " -> throws " + this.type.toReadable(TypeString.Context.CONCRETE) + ">";
        }
    }

    record MethodRefThrows(MethodReferenceModel model, TypeConcrete type) implements ConstraintFormula {
        @Override
        public String toString() {
            return "<" + this.model.toString() + " -> throws " + this.type.toReadable(TypeString.Context.CONCRETE) + ">";
        }
    }

}
