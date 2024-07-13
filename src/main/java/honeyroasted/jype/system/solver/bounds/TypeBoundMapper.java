package honeyroasted.jype.system.solver.bounds;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public interface TypeBoundMapper {

    default int arity() {
        return 1;
    }

    boolean accepts(TypeBound.Result.Builder constraint);

    void map(Set<TypeBound.Result.Builder> results, TypeBound.Result.Builder... constraints);

    default Set<TypeBound.Result.Builder> map(TypeBound.Result.Builder... constraints) {
        Set<TypeBound.Result.Builder> results = new LinkedHashSet<>();
        this.map(results, constraints);
        return results;
    }

    default Set<TypeBound.Result.Builder> map(Set<TypeBound.Result.Builder> constraints) {
        return this.map(constraints.toArray(TypeBound.Result.Builder[]::new));
    }

    default <T> void addAll(Collection<? super T> collection, T... arr) {
        Collections.addAll(collection, arr);
    }

}
