package honeyroasted.jype;

import honeyroasted.almonds.solver.SolveResult;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.List;

import static honeyroasted.jype.system.solver.constraints.TypeConstraints.Compatible.Context.*;

public class Test {

    public static void main(String[] args) {
        TypeSystem system = TypeSystem.SIMPLE_RUNTIME;

        ClassReference list = system.tryResolve(List.class);

        VarType vt = list.typeParameters().get(0);
        MetaVarType mvt = system.typeFactory().newMetaVarType(vt.name());

        Type subtype = list.parameterized(vt);
        Type supertype = list.parameterized(system.<ArgumentType>tryResolve(String.class));

        SolveResult solve = system.operations().inferenceSolver()
                .bind(new TypeConstraints.Infer(mvt, vt),
                        new TypeConstraints.Compatible(subtype, LOOSE_INVOCATION, supertype))
                .solve();
        
        solve.allTrackedConstraints().stream().filter(tr -> tr.constraint() instanceof TypeConstraints.Instantiation).forEach(System.out::println);
        System.out.println(solve);
    }

}
