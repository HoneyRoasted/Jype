package honeyroasted.jype.system.solver.bounds;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TypeBoundMapperApplier {
    private List<TypeBoundMapper> mappers;

    public TypeBoundMapperApplier(List<TypeBoundMapper> mappers) {
        this.mappers = mappers;
    }

    public Set<TypeBound.Result.Builder> process(Set<TypeBound.Result.Builder> constraints) {
        Set<TypeBound.Result.Builder> previous = new LinkedHashSet<>();
        Set<TypeBound.Result.Builder> current = new LinkedHashSet<>(constraints);

        while (!previous.equals(current)) {
            previous = current;
            current = new LinkedHashSet<>();

            for (TypeBoundMapper mapper : this.mappers) {
                Set<TypeBound.Result.Builder> processing = previous.stream().filter(mapper::accepts).collect(Collectors.toCollection(LinkedHashSet::new));

                Set<TypeBound.Result.Builder> results = current;
                consumeSubsets(processing, mapper.arity(), arr -> mapper.map(results, arr));
            }
        }

        return constraints;
    }

    private static void consumeSubsets(Set<TypeBound.Result.Builder> processing, int size, Consumer<TypeBound.Result.Builder[]> baseCase) {
        if (size <= 0 || size == processing.size()) {
            baseCase.accept(processing.toArray(new TypeBound.Result.Builder[0]));
        } else if (size < processing.size()) {
            TypeBound.Result.Builder[] mem = new TypeBound.Result.Builder[size];
            TypeBound.Result.Builder[] input = processing.toArray(TypeBound.Result.Builder[]::new);
            int[] subset = IntStream.range(0, size).toArray();

            consumeSubset(mem, input, subset, baseCase);
            while (true) {
                int i;
                for (i = size - 1; i >= 0 && subset[i] == input.length - size + i; i--);
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
