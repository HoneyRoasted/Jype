package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.Type;

import java.util.LinkedHashSet;
import java.util.Set;

public class FindAllKnownSupertypes implements TypeOperation<Type, Set<Type>> {

    @Override
    public Set<Type> apply(TypeSystem system, Type type) {
            Set<Type> result = new LinkedHashSet<>();
            allKnownSupertypes(type, result);
            return result;
    }

    private void allKnownSupertypes(Type type, Set<Type> building) {
        if (!building.contains(type)) {
            building.add(type);
            type.knownDirectSupertypes().forEach(t -> allSupertypes(t, building));
        }
    }

    private void allSupertypes(Type type, Set<Type> types) {
        if (!types.contains(type)) {
            types.add(type);
            type.knownDirectSupertypes().forEach(t -> allSupertypes(t, types));
        }
    }
}
