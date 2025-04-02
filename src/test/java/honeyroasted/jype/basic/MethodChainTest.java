package honeyroasted.jype.basic;

import honeyroasted.almonds.ConstraintTree;
import honeyroasted.jype.system.JExpressionInformation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JType;

import java.util.Collections;
import java.util.List;

import static honeyroasted.jype.system.solver.constraints.JTypeConstraints.Compatible.Context.*;

public class MethodChainTest {

    public static void main(String[] args) {
        JTypeSystem system = JTypeSystem.RUNTIME_REFLECTION;

        JClassReference declaring = system.tryResolve(MethodChainTest.class);

        JExpressionInformation constStr = new JExpressionInformation.Constant.Simple("Hello World");
        JExpressionInformation call1 = new JExpressionInformation.Invocation.MethodInvocation.Simple<>(declaring, declaring, "getIt",
                List.of(constStr), Collections.emptyList());

        JExpressionInformation call2 = new JExpressionInformation.MethodInvocation.Simple<>(declaring, call1, "getBytes",
                Collections.emptyList(), Collections.emptyList());

        JType targetType = system.tryResolve(byte[].class);

        JTypeConstraints.ExpressionCompatible constraint = new JTypeConstraints.ExpressionCompatible(call2, ASSIGNMENT, targetType);

        ConstraintTree solved = system.operations().inferenceSolver()
                .bind(constraint)
                .solve();

        System.out.println(solved.toString(true));
    }

    private static <T> T getIt(T value) {
        return value;
    }

}
