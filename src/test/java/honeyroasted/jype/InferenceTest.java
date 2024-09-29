package honeyroasted.jype;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.JExpressionInformation;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;

import java.util.ArrayList;
import java.util.List;

import static honeyroasted.jype.system.solver.constraints.JTypeConstraints.Compatible.Context.*;

public class InferenceTest {

    public static void main(String[] args) {
        JTypeSystem system = JTypeSystem.RUNTIME_REFLECTION;

        JClassReference declaring = system.tryResolve(InferenceTest.class);
        List<JArgumentType> explicitTypeArguments = List.of();

        JClassReference integer = system.constants().intBox();
        List<JExpressionInformation> intParams = List.of(new JExpressionInformation.Constant.Simple(1));

        JExpressionInformation.Instantiation subInst = new JExpressionInformation.Instantiation.Simple(declaring, integer, intParams, explicitTypeArguments);

        JClassReference type = system.tryResolve(ArrayList.class);
        List<JExpressionInformation> parameters = List.of(subInst);

        JExpressionInformation.Instantiation instantiation = new JExpressionInformation.Instantiation.Simple(declaring, type, parameters, explicitTypeArguments);
        JClassType targetType = system.<JClassReference>tryResolve(List.class).parameterized(system.<JArgumentType>tryResolve(String.class));

        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(instantiation, ASSIGNMENT, targetType);


        double sum = 0;
        int times = 10;
        for (int i = 0; i < times; i++) {
            long curr = System.currentTimeMillis();
            system.operations().inferenceSolver()
                    .bind(constraint)
                    .solve();
            sum += (System.currentTimeMillis() - curr);
        }

        System.out.println("AVERAGE MS: " + (sum / times));

        ConstraintTree solve = system.operations().inferenceSolver()
                .bind(constraint)
                .solve();

        System.out.println(solve.toString(true));
    }

}
