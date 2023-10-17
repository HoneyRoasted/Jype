package honeyroasted.jype.system.solver.solvers.inference.expression;

import honeyroasted.jype.type.InstantiableType;
import honeyroasted.jype.type.Type;

import java.util.List;

public interface ExpressionInformation {

    String simpleName();

    interface Standalone extends ExpressionInformation {
        Type type();
    }

    interface Constant extends Standalone {

        Object value();

    }

    interface Poly extends ExpressionInformation {
        List<ExpressionInformation> children();
    }

    interface Instantiation extends ExpressionInformation {
        InstantiableType type();

        List<ExpressionInformation> parameters();
    }

    interface Invocation extends ExpressionInformation {
        ExpressionInformation source();

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
