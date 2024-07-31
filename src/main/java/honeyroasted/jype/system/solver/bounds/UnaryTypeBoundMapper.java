package honeyroasted.jype.system.solver.bounds;

import java.util.List;

public interface UnaryTypeBoundMapper<T extends TypeBound> extends TypeBoundMapper {

    @Override
    default void map(List<TypeBound.Result.Builder> bounds, List<TypeBound.Result.Builder> constraints, TypeBound.Classification classification, TypeBound.Result.Builder... input) {
        map(bounds, constraints, classification, input[0], (T) input[0].bound());
    }

    void map(List<TypeBound.Result.Builder> bounds, List<TypeBound.Result.Builder> constraints, TypeBound.Classification classification, TypeBound.Result.Builder constraint, T bound);

}
