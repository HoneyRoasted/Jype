package honeyroasted.jype.system.solver.constraints;

import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.jype.system.expression.ExpressionInformation;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.Type;

import java.util.function.Function;

public interface TypeContext {
    record TypeMapper(Function<ConstraintBranch, Function<Type, Type>> mapper) {
        public static final TypeMapper NO_OP = new TypeMapper(cn -> Function.identity());
    }

    record ChosenMethod(ExpressionInformation.Invocation expression, MethodReference chosen, TypeConstraints.Compatible.Context context, boolean vararg) {

    }

    record TriedAdvancedReduction() {

    }
}
