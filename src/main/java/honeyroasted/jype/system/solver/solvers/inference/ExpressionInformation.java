package honeyroasted.jype.system.solver.solvers.inference;

import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.Type;

import java.util.List;

public interface ExpressionInformation {

    record Standalone(Type type) implements ExpressionInformation {}

    record Poly(List<ExpressionInformation> children) implements ExpressionInformation {}

    record Instantiation(ClassReference reference, List<Type> explicitTypeArguments, List<Type> parameters) implements ExpressionInformation {}

    record Invocation(MethodReference reference, List<Type> explicitTypeArguments, List<Type> parameters) implements ExpressionInformation {}

    record Lambda(List<Type> explicitParameterTypes, int parameters, ExpressionInformation returnExpression) implements ExpressionInformation {}

    record InvocationReference(ExpressionInformation source, List<Type> explicitParameterTypes, String methodName) implements ExpressionInformation {}
}
