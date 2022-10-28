package honeyroasted.jype.system.solver.inference.model;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;

public record StandaloneExpressionModel(TypeConcrete type) implements ExpressionModel {
    @Override
    public String toString() {
        return "Expression(" + type.toReadable(TypeString.Context.CONCRETE) + ")";
    }
}
