import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.TypeToken;
import honeyroasted.jype.system.solver.impl.ForceResolveTypeSolver;

import java.util.Map;

public class Test {

    public static void main(String[] args) {
        TypeSystem system = new TypeSystem();

        TypeConcrete a = system.token(new TypeToken<Map<String, ? extends Number>>(){});
        TypeConcrete b = system.token(new TypeToken<Map<String, Integer>>(){});

        System.out.println(new ForceResolveTypeSolver(system)
                .constrain(a, b)
                .solve()
                .verification());
    }

}
