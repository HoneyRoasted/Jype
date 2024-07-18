package honeyroasted.jype.system.solver.bounds;

import java.util.Set;

public interface BinaryTypeBoundMapper<T extends TypeBound, K extends TypeBound> extends TypeBoundMapper {

    @Override
    default void map(Set<TypeBound.Result.Builder> results, TypeBound.Result.Builder... constraints) {
        map(results, constraints[0], (T) constraints[0].bound(), constraints[1], (K) constraints[1].bound());
    }

    void map(Set<TypeBound.Result.Builder> results, TypeBound.Result.Builder leftConstraint, T leftBound,
                                                        TypeBound.Result.Builder rightConstraint, K rightBound);


}