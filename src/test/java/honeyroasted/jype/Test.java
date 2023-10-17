package honeyroasted.jype;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.resolver.reflection.TypeToken;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.solvers.CompatibilityTypeSolver;
import honeyroasted.jype.type.Type;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class Test {

    public static void main(String[] args) throws NoSuchMethodException {
        Function<String, String> upper = s -> s.toUpperCase();

        TypeSystem system = TypeSystem.RUNTIME;
        TypeSolver.Result result = new CompatibilityTypeSolver()
                .bind(
                        new TypeBound.Compatible(resolve(new TypeToken<LinkedHashMap<String, Integer>>() {}), resolve(new TypeToken<Map<String, Integer>>() {}))
                )
                .solve(system);

        System.out.println(result.toString(true));
    }

    private static Type resolve(TypeToken<?> token) {
        return TypeSystem.RUNTIME.resolve(token).get();
    }

    private static Type resolve(java.lang.reflect.Type token) {
        return TypeSystem.RUNTIME.resolve(token).get();
    }


}
