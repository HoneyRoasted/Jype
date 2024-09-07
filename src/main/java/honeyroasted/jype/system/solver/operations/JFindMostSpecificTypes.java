package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JType;

import java.util.LinkedHashSet;
import java.util.Set;

public class JFindMostSpecificTypes implements JTypeOperation<Set<JType>, Set<JType>> {

    @Override
    public Set<JType> apply(JTypeSystem system, Set<JType> types) {
        Set<JType> result = new LinkedHashSet<>();
        for (JType curr : types) {
            if (types.stream().noneMatch(t ->
                    t != curr && system.operations().isSubtype(t, curr))) {
                result.add(curr);
            }
        }
        return result;
    }

}
