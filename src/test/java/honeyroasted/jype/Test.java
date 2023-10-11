package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.reflection.ReflectionTypeResolution;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.solvers.AssignabilityTypeSolver;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.function.Function;

public class Test {

    public static void main(String[] args) throws NoSuchMethodException {
        Function<String, String> upper = s -> s.toUpperCase();

        TypeSystem system = TypeSystem.RUNTIME;
        ClassReference ref = (ClassReference) system.resolve(upper.getClass()).get();
        System.out.println(ref);
        System.out.println(ReflectionTypeResolution.getReflectionType(ref));
    }

}
