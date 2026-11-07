package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JMemberLookup {

    private JMemberLookup() {
    }

    public static Set<JClassType> findClassTypes(JType type) {
        if (type instanceof JClassType ct) {
            return Set.of(ct);
        } else {
            Set<JClassType> result = new LinkedHashSet<>();
            Set<JType> visited = new LinkedHashSet<>();
            visited.add(type);
            Set<JType> frontier = Set.of(type);

            while (!frontier.isEmpty()) {
                Set<JType> next = new LinkedHashSet<>();

                for (JType curr : frontier) {
                    for (JType supertype : curr.knownDirectSupertypes()) {
                        if (supertype instanceof JClassType ct) {
                            result.add(ct);
                        } else if (visited.add(supertype)) {
                            next.add(supertype);
                        }
                    }
                }

                frontier = next;
            }

            return result;
        }
    }

    // JLS §11.1.1
    public static boolean isUncheckedException(JType type) {
        JTypeSystem system = type.typeSystem();
        return system.operations().isSubtype(type, system.constants().runtimeException())
                || system.operations().isSubtype(type, system.constants().error());
    }

    // JLS §11.2.2
    public static List<JType> checkedExceptions(JMethodReference ref) {
        return ref.exceptionTypes().stream().filter(t -> !isUncheckedException(t)).toList();
    }
}
