package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JType;

import java.util.Set;

public class JFindGreatestLowerBound implements JTypeOperation<Set<JType>, JType> {

    @Override
    public JType apply(JTypeSystem system, Set<JType> types) {
        return JIntersectionType.of(types, system);
    }

}
