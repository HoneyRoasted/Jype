package honeyroasted.jype.system.solver.solvers.inference.helper;

import honeyroasted.jype.type.Type;

import java.util.LinkedHashSet;
import java.util.Set;

public class TypeLeastUpperBound {



    private static Set<Type> allSupertypes(Type type) {
        Set<Type> result = new LinkedHashSet<>();
        allSupertypes(type, result);
        return result;
    }

    private static void allSupertypes(Type type, Set<Type> types) {
        if (!types.contains(type)) {
            types.add(type);
            type.knownDirectSupertypes().forEach(t -> allSupertypes(t, types));
        }
    }

}
