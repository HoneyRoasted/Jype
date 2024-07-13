package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.Type;

import java.util.Set;

public class FindMostSpecificType implements TypeOperation<Set<Type>, Type> {

    @Override
    public Type apply(TypeSystem system, Set<Type> types) {
        return IntersectionType.of(system.operations().findMostSpecificTypes(types), system);
    }


}
