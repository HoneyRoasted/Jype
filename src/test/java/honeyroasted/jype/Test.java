package honeyroasted.jype;

import honeyroasted.almonds.solver.SolveResult;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.expression.ExpressionInformation;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.Type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static honeyroasted.jype.system.solver.constraints.TypeConstraints.Compatible.Context.*;

public class Test {

    public static void main(String[] args) throws IOException {
        TypeSystem system = TypeSystem.SIMPLE_RUNTIME;

        ClassReference declaring = system.tryResolve(Test.class);
        List<ArgumentType> explicitTypeArguments = List.of();
        List<ExpressionInformation> parameters = List.of();
        ClassReference source = system.tryResolve(Collections.class);

        ClassReference list = system.tryResolve(List.class);
        ClassReference string = system.tryResolve(String.class);

        Type target = list.parameterized(string);

        ExpressionInformation.Invocation<ClassReference> invocation =
                new SimpleInvocation<>("emptyList", declaring, parameters, explicitTypeArguments, source);

        TypeConstraints.ExpressionCompatible constraint = new TypeConstraints.ExpressionCompatible(invocation, LOOSE_INVOCATION, target);

        SolveResult solve = system.operations().inferenceSolver()
                .bind(constraint)
                .solve();

        System.out.println(solve.toString(true));
    }

    record SimpleInst(ClassReference declaring, ClassReference type, List<ExpressionInformation> parameters,
                      List<ArgumentType> explicitTypeArguments) implements ExpressionInformation.Instantiation {

        @Override
        public String simpleName() {
            return "new " + this.type.simpleName() + (this.explicitTypeArguments.isEmpty() ? "" : "<" + explicitTypeArguments.stream().map(ArgumentType::simpleName).collect(Collectors.joining(", ")) + ">") +
                    "(" + this.parameters.stream().map(ExpressionInformation::simpleName).collect(Collectors.joining(", ")) + ")";
        }
    }

    record SimpleInvocation<T>(String name, ClassReference declaring, List<ExpressionInformation> parameters,
                               List<ArgumentType> explicitTypeArguments,
                               T source) implements ExpressionInformation.Invocation<T> {

        @Override
        public String simpleName() {
            return (this.source instanceof ClassReference cr ? cr.simpleName() :
                    this.source instanceof ExpressionInformation ei ? ei.simpleName() :
                            this.source) + "." + this.name + "(" + (this.explicitTypeArguments.isEmpty() ? "" : "<" + explicitTypeArguments.stream().map(ArgumentType::simpleName).collect(Collectors.joining(", ")) + ">") +
                    "(" + this.parameters.stream().map(ExpressionInformation::simpleName).collect(Collectors.joining(", ")) + ")";
        }
    }

}
