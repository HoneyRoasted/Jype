package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.Type;

import java.util.LinkedHashSet;
import java.util.Set;

public class FindMostSpecificTypes implements TypeOperation<Set<Type>, Set<Type>> {

    @Override
    public Set<Type> apply(TypeSystem system, Set<Type> types) {
        Set<Type> result = new LinkedHashSet<>();
        for (Type curr : types) {
            if (types.stream().noneMatch(t ->
                    t != curr && system.operations().isSubtype(t, curr))) {
                result.add(curr);
            }
        }
        return result;
    }

}
