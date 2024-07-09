package honeyroasted.jype.system.solver.bounds;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public interface TypeBoundMapper {

    default int arity() {
        return 1;
    }

    boolean accepts(TypeBound.Result.Builder constraint);

    void map(Set<TypeBound.Result.Builder> results, TypeBound.Result.Builder... constraints);

    default <T> void addAll(Collection<? super T> collection, T... arr) {
        Collections.addAll(collection, arr);
    }

}
