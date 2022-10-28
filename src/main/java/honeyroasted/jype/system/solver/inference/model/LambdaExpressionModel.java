package honeyroasted.jype.system.solver.inference.model;

import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.TypeString;

import java.util.List;
import java.util.stream.Collectors;

public record LambdaExpressionModel(List<TypeConcrete> parameters, TypeConcrete returnType) implements ExpressionModel {
    @Override
    public String toString() {
        return "Lambda(" + this.parameters.stream().map(t -> t.toReadable(TypeString.Context.CONCRETE).value()).collect(Collectors.joining(", "))
                + " => " + this.returnType.toReadable(TypeString.Context.CONCRETE).value() + ")";
    }
}
