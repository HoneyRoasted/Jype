package honeyroasted.jype.system.solver.bounds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface TypeBoundMapper {

    default int arity() {
        return 1;
    }

    boolean accepts(TypeBound.Result.Builder constraint);

    void map(List<TypeBound.Result.Builder> results, TypeBound.Result.Builder... constraints);

    default List<TypeBound.Result.Builder> map(TypeBound.Result.Builder... constraints) {
        List<TypeBound.Result.Builder> results = new ArrayList<>();
        this.map(results, constraints);
        return results;
    }

    default List<TypeBound.Result.Builder> map(List<TypeBound.Result.Builder> constraints) {
        return this.map(constraints.toArray(TypeBound.Result.Builder[]::new));
    }

    default <T> void addAll(Collection<? super T> collection, T... arr) {
        Collections.addAll(collection, arr);
    }

}
