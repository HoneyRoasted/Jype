package honeyroasted.jype.system.solver.bounds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class TypeBoundMapperApplier implements TypeBoundMapper {
    private List<TypeBoundMapper> mappers;

    public TypeBoundMapperApplier(List<TypeBoundMapper> mappers) {
        this.mappers = mappers;
    }

    @Override
    public boolean accepts(TypeBound.Result.Builder constraint) {
        return true;
    }

    @Override
    public int arity() {
        return -1;
    }

    @Override
    public void map(List<TypeBound.Result.Builder> results, TypeBound.Result.Builder... constraints) {
        List<TypeBound.Result.Builder> constraintSet = new ArrayList<>();
        Collections.addAll(constraintSet, constraints);

        results.addAll(this.process(constraintSet));
    }

    public List<TypeBound.Result.Builder> process(List<TypeBound.Result.Builder> constraints) {
        List<TypeBound.Result.Builder> compare = new ArrayList<>();

        List<TypeBound.Result.Builder> previous = new ArrayList<>();
        List<TypeBound.Result.Builder> processing = new ArrayList<>();
        List<TypeBound.Result.Builder> current = new ArrayList<>(constraints);

        while (!compare.equals(current)) {
            compare.clear();
            compare.addAll(current);

            for (TypeBoundMapper mapper : this.mappers) {
                previous.clear();
                previous.addAll(current);

                current.clear();
                processing.clear();

                for (TypeBound.Result.Builder constraint : previous) {
                    if (mapper.accepts(constraint)) {
                        processing.add(constraint);
                    } else {
                        current.add(constraint);
                    }
                }

                if (!processing.isEmpty()) {
                    consumeSubsets(processing, mapper.arity(), arr -> mapper.map(current, arr));
                }
            }
        }
        return current;
    }

    private static void consumeSubsets(List<TypeBound.Result.Builder> processing, int size, Consumer<TypeBound.Result.Builder[]> baseCase) {
        if (size <= 0 || size == processing.size()) {
            baseCase.accept(processing.toArray(new TypeBound.Result.Builder[0]));
        } else if (size < processing.size()) {
            TypeBound.Result.Builder[] mem = new TypeBound.Result.Builder[size];
            TypeBound.Result.Builder[] input = processing.toArray(TypeBound.Result.Builder[]::new);
            int[] subset = IntStream.range(0, size).toArray();

            consumeSubset(mem, input, subset, baseCase);
            while (true) {
                int i;
                for (i = size - 1; i >= 0 && subset[i] == input.length - size + i; i--) ;
                if (i < 0) break;

                subset[i]++;
                for (++i; i < size; i++) {
                    subset[i] = subset[i - 1] + 1;
                }
                consumeSubset(mem, input, subset, baseCase);
            }
        }
    }

    private static void consumeSubset(TypeBound.Result.Builder[] mem, TypeBound.Result.Builder[] input, int[] subset, Consumer<TypeBound.Result.Builder[]> baseCase) {
        for (int i = 0; i < subset.length; i++) {
            mem[i] = input[subset[i]];
        }
        baseCase.accept(mem);
    }

}
