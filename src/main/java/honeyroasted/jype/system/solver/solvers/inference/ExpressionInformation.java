package honeyroasted.jype.system.solver.solvers.inference;

import honeyroasted.jype.type.InstantiableType;
import honeyroasted.jype.type.MethodType;
import honeyroasted.jype.type.Type;

import java.util.List;

public interface ExpressionInformation {

    interface Standalone extends ExpressionInformation {
        Type type();
    }

    interface Poly extends ExpressionInformation {
        List<ExpressionInformation> children();
    }

    interface Instantiation extends ExpressionInformation {
        InstantiableType type();

        List<ExpressionInformation> parameters();
    }

    interface Invocation extends ExpressionInformation {
        MethodType type();

        List<ExpressionInformation> parameters();
    }

    interface Lambda extends ExpressionInformation {
        ExpressionInformation body();

        List<Type> explicitParameterTypes();

        int parameterCount();

        boolean implicitReturn();
    }

    interface InvocationReference extends ExpressionInformation {
        ExpressionInformation source();
        String methodName();
        List<Type> explicitParameterTypes();
    }

}