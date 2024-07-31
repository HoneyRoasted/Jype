package honeyroasted.jype.system.solver.bounds;

import java.util.Set;

public interface UnaryTypeBoundMapper<T extends TypeBound> extends TypeBoundMapper {

    @Override
    default void map(Set<TypeBound.Result.Builder> bounds, Set<TypeBound.Result.Builder> constraints, TypeBound.Result.Builder... input) {
        map(bounds, input[0], (T) input[0].bound());
    }

    void map(Set<TypeBound.Result.Builder> results, TypeBound.Result.Builder constraint, T bound);

}
