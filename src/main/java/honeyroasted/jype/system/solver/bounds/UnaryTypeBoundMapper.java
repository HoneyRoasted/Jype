package honeyroasted.jype.system.solver.bounds;

import java.util.Set;

public interface UnaryTypeBoundMapper<T extends TypeBound> extends TypeBoundMapper {

    @Override
    default void map(Set<TypeBound.Result.Builder> results, TypeBound.Result.Builder... constraints) {
        map(results, constraints[0], (T) constraints[0].bound());
    }

    void map(Set<TypeBound.Result.Builder> results, TypeBound.Result.Builder constraint, T bound);

}
