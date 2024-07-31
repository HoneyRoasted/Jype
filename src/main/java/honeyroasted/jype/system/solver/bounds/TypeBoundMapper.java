package honeyroasted.jype.system.solver.bounds;

import honeyroasted.jype.modify.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public interface TypeBoundMapper {

    default int arity() {
        return 1;
    }

    default TypeBound.Classification classification() {
        return TypeBound.Classification.CONSTRAINT;
    }

    boolean accepts(TypeBound.Result.Builder constraint);

    void map(Set<TypeBound.Result.Builder> bounds, Set<TypeBound.Result.Builder> constraints, TypeBound.Result.Builder... input); 

    default Pair<Set<TypeBound.Result.Builder>, Set<TypeBound.Result.Builder>> map(TypeBound.Result.Builder... input) {
        Set<TypeBound.Result.Builder> bounds = new LinkedHashSet<>();
        Set<TypeBound.Result.Builder> constraints = new LinkedHashSet<>();
        this.map(bounds, constraints, input);
        return Pair.of(bounds, constraints);
    }

    default Pair<Set<TypeBound.Result.Builder>, Set<TypeBound.Result.Builder>> map(Set<TypeBound.Result.Builder> constraints) {
        return this.map(constraints.toArray(TypeBound.Result.Builder[]::new));
    }

    default <T> void addAll(Collection<? super T> collection, T... arr) {
        Collections.addAll(collection, arr);
    }

}
