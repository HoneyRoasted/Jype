package honeyroasted.jype.system.solver.bounds;

import java.util.List;

public interface UnaryTypeBoundMapper<T extends TypeBound> extends TypeBoundMapper {

    @Override
    default void map(List<TypeBound.Result.Builder> results, TypeBound.Result.Builder... constraints) {
        map(results, constraints[0], (T) constraints[0].bound());
    }

    void map(List<TypeBound.Result.Builder> results, TypeBound.Result.Builder constraint, T bound);

}
