package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.solvers.AssignabilityTypeSolver;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.function.Function;

public class Test {

    public static void main(String[] args) throws NoSuchMethodException {
        TypeSystem system = new TypeSystem();
        ClassType ct2 = system.<ClassType>resolve(new TypeToken<CharSequence>() {}).get();
        ClassType ct3 = system.<ClassType>resolve(new TypeToken<String>(){}).get();

        System.out.println(new AssignabilityTypeSolver()
                .bind(new TypeBound.Compatible(ct3, ct2)).solve(system));
    }

}
