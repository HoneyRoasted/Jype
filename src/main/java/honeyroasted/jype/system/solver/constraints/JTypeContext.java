package honeyroasted.jype.system.solver.constraints;

import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;

import java.util.function.Function;

public interface JTypeContext {
    record JTypeMapper(Function<ConstraintBranch, Function<JType, JType>> mapper) {
        public static final JTypeMapper NO_OP = new JTypeMapper(cn -> Function.identity());
    }

    record ChosenMethod(JExpressionInformation.Invocation expression, JMethodReference chosen, JTypeConstraints.Compatible.Context context, boolean vararg) {

    }
}
