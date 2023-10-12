package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.reflection.ReflectionTypeResolution;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.solvers.AssignabilityTypeSolver;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Test {

    public static void main(String[] args) throws NoSuchMethodException {
        Function<String, String> upper = s -> s.toUpperCase();

        TypeSystem system = TypeSystem.RUNTIME;
        TypeSolver.Result result = new AssignabilityTypeSolver()
                .bind(
                        new TypeBound.Compatible(resolve(Integer.class), resolve(Object.class)),
                        new TypeBound.Compatible(resolve(new TypeToken<LinkedHashMap<String, Integer>>() {}), resolve(new TypeToken<Map<String, Integer>>() {})),
                        new TypeBound.Compatible(resolve(new TypeToken<ArrayList<String>>() {}), resolve(new TypeToken<Collection<? extends CharSequence>>() {})),
                        new TypeBound.Compatible(resolve(ArrayList.class), resolve(Number.class))
                )
                .solve(system);

        System.out.println(result);
    }

    private static Type resolve(TypeToken<?> token) {
        return TypeSystem.RUNTIME.resolve(token).get();
    }

    private static Type resolve(java.lang.reflect.Type token) {
        return TypeSystem.RUNTIME.resolve(token).get();
    }


}
