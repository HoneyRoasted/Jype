package honeyroasted.jype.system.solver.constraints;

import honeyroasted.jype.system.JExpressionInformation;
import honeyroasted.jype.type.JMethodReference;

public interface JTypeContext {
    record ChosenMethod(JExpressionInformation.Invocation expression, JMethodReference chosen, JTypeConstraints.Compatible.Context context, boolean vararg) {

    }
}
