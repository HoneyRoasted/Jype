package honeyroasted.jype;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.expression.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static honeyroasted.jype.system.solver.constraints.JTypeConstraints.Compatible.Context.*;

public class Test {

    public static void main(String[] args) {
        JTypeSystem system = JTypeSystem.RUNTIME_REFLECTION;

        JClassReference declaring = system.tryResolve(Test.class);
        List<JArgumentType> explicitTypeArguments = List.of();

        JClassReference integer = system.constants().intBox();
        List<JExpressionInformation> intParams = List.of(JExpressionInformation.of(system.constants().intType()));

        JExpressionInformation.Instantiation subInst = new SimpleInst(declaring, integer, intParams, explicitTypeArguments);

        JClassReference type = system.tryResolve(ArrayList.class);
        List<JExpressionInformation> parameters = List.of(subInst);

        JExpressionInformation.Instantiation instantiation = new SimpleInst(declaring, type, parameters, explicitTypeArguments);
        JClassType targetType = system.<JClassReference>tryResolve(List.class).parameterized(system.<JArgumentType>tryResolve(String.class));

        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(instantiation, LOOSE_INVOCATION, targetType);

        ConstraintTree solve = system.operations().inferenceSolver()
                .bind(constraint)
                .solve();

        System.out.println(solve.toString(false));
    }

    record SimpleInst(JClassReference declaring, JClassReference type, List<JExpressionInformation> parameters, List<JArgumentType> explicitTypeArguments) implements JExpressionInformation.Instantiation {

        @Override
        public String simpleName() {
            return "new " + this.type.simpleName() + (this.explicitTypeArguments.isEmpty() ? "" : "<" + explicitTypeArguments.stream().map(JArgumentType::simpleName).collect(Collectors.joining(", ")) + ">") +
                    "(" + this.parameters.stream().map(JExpressionInformation::simpleName).collect(Collectors.joining(", ")) + ")";
        }

        @Override
        public String toString() {
            return simpleName();
        }
    }

}
