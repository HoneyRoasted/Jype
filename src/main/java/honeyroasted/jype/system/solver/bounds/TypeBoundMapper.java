package honeyroasted.jype.system.solver.bounds;

import honeyroasted.jype.modify.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface TypeBoundMapper {

    default int arity() {
        return 1;
    }

    default TypeBound.Classification classification() {
        return TypeBound.Classification.CONSTRAINT;
    }

    boolean accepts(TypeBound.Result.Builder constraint);

    void map(List<TypeBound.Result.Builder> bounds, List<TypeBound.Result.Builder> constraints, TypeBound.Classification classification, TypeBound.Result.Builder... input);

    default Pair<List<TypeBound.Result.Builder>, List<TypeBound.Result.Builder>> map(TypeBound.Classification classification, TypeBound.Result.Builder... input) {
        List<TypeBound.Result.Builder> bounds = new ArrayList<>();
        List<TypeBound.Result.Builder> constraints = new ArrayList<>();
        this.map(bounds, constraints, classification, input);
        return Pair.of(bounds, constraints);
    }

    default Pair<List<TypeBound.Result.Builder>, List<TypeBound.Result.Builder>> map(TypeBound.Classification classification, List<TypeBound.Result.Builder> constraints) {
        return this.map(classification, constraints.toArray(TypeBound.Result.Builder[]::new));
    }

    default <T> void addAll(Collection<? super T> collection, T... arr) {
        Collections.addAll(collection, arr);
    }

}
