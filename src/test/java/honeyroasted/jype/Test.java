package honeyroasted.jype;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.expression.ExpressionInformation;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static honeyroasted.jype.system.solver.constraints.TypeConstraints.Compatible.Context.*;

public class Test {

    public static void main(String[] args) {
        TypeSystem system = TypeSystem.SIMPLE_RUNTIME;

        ClassReference declaring = system.tryResolve(Test.class);
        List<ArgumentType> explicitTypeArguments = List.of();

        ClassReference integer = system.constants().intBox();
        List<ExpressionInformation> intParams = List.of(ExpressionInformation.of(system.constants().intType()));

        ExpressionInformation.Instantiation subInst = new SimpleInst(declaring, integer, intParams, explicitTypeArguments);

        ClassReference type = system.tryResolve(ArrayList.class);
        List<ExpressionInformation> parameters = List.of(subInst);

        ExpressionInformation.Instantiation instantiation = new SimpleInst(declaring, type, parameters, explicitTypeArguments);
        ClassType targetType = system.<ClassReference>tryResolve(List.class).parameterized(system.<ArgumentType>tryResolve(String.class));

        TypeConstraints.ExpressionCompatible constraint = new TypeConstraints.ExpressionCompatible(instantiation, LOOSE_INVOCATION, targetType);

        ConstraintTree solve = system.operations().inferenceSolver()
                .bind(constraint)
                .solve();

        System.out.println(solve.toString(true));
    }

    record SimpleInst(ClassReference declaring, ClassReference type, List<ExpressionInformation> parameters, List<ArgumentType> explicitTypeArguments) implements ExpressionInformation.Instantiation {

        @Override
        public String simpleName() {
            return "new " + this.type.simpleName() + (this.explicitTypeArguments.isEmpty() ? "" : "<" + explicitTypeArguments.stream().map(ArgumentType::simpleName).collect(Collectors.joining(", ")) + ">") +
                    "(" + this.parameters.stream().map(ExpressionInformation::simpleName).collect(Collectors.joining(", ")) + ")";
        }

        @Override
        public String toString() {
            return simpleName();
        }
    }

}
