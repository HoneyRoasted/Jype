package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JType;

import java.util.LinkedHashSet;
import java.util.Set;

public class JFindAllKnownSupertypes implements JTypeOperation<JType, Set<JType>> {

    @Override
    public Set<JType> apply(JTypeSystem system, JType type) {
        Set<JType> result = new LinkedHashSet<>();
        allKnownSupertypes(type, result);
        return result;
    }

    private void allKnownSupertypes(JType type, Set<JType> building) {
        if (!building.contains(type)) {
            building.add(type);
            type.knownDirectSupertypes().forEach(t -> allSupertypes(t, building));
        }
    }

    private void allSupertypes(JType type, Set<JType> types) {
        if (!types.contains(type)) {
            types.add(type);
            type.knownDirectSupertypes().forEach(t -> allSupertypes(t, types));
        }
    }
}
