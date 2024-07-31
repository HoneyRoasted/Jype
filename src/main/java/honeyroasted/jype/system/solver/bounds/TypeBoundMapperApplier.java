package honeyroasted.jype.system.solver.bounds;

import honeyroasted.jype.modify.Pair;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
    public void map(Set<TypeBound.Result.Builder> bounds, Set<TypeBound.Result.Builder> constraints, TypeBound.Result.Builder... input) {
        Set<TypeBound.Result.Builder> constraintSet = new LinkedHashSet<>();
        Collections.addAll(constraintSet, input);

        Pair<Set<TypeBound.Result.Builder>, Set<TypeBound.Result.Builder>> result = this.process(new LinkedHashSet<>(), constraintSet);
        bounds.addAll(result.left());
        constraints.addAll(result.right());
    }

    public Pair<Set<TypeBound.Result.Builder>, Set<TypeBound.Result.Builder>> process(Set<TypeBound.Result.Builder> bounds, Set<TypeBound.Result.Builder> constraints) {
        Set<TypeBound.Result.Builder> compareConstraints = new LinkedHashSet<>();
        Set<TypeBound.Result.Builder> previousConstraints = new LinkedHashSet<>();
        Set<TypeBound.Result.Builder> processingConstraints = new LinkedHashSet<>();
        Set<TypeBound.Result.Builder> currentConstraints = new LinkedHashSet<>(constraints);

        Set<TypeBound.Result.Builder> compareBounds = new LinkedHashSet<>();
        Set<TypeBound.Result.Builder> previousBounds = new LinkedHashSet<>();
        Set<TypeBound.Result.Builder> processingBounds = new LinkedHashSet<>();
        Set<TypeBound.Result.Builder> currentBounds = new LinkedHashSet<>(bounds);


        while (!compareConstraints.equals(currentConstraints) && !compareBounds.equals(currentBounds)) {
            compareConstraints.clear();
            compareConstraints.addAll(currentConstraints);

            compareBounds.clear();
            compareBounds.addAll(currentBounds);

            for (TypeBoundMapper mapper : this.mappers) {
                previousConstraints.clear();
                previousConstraints.addAll(currentConstraints);
                currentConstraints.clear();
                processingConstraints.clear();

                previousBounds.clear();
                previousBounds.addAll(currentBounds);
                currentBounds.clear();
                processingBounds.clear();


                if (mapper.classification() == TypeBound.Classification.CONSTRAINT || mapper.classification() == TypeBound.Classification.BOTH) {
                    for (TypeBound.Result.Builder constraint : previousConstraints) {
                        if (mapper.accepts(constraint)) {
                            processingConstraints.add(constraint);
                        } else {
                            currentConstraints.add(constraint);
                        }
                    }

                    if (!processingConstraints.isEmpty()) {
                        consumeSubsets(processingConstraints, mapper.arity(), arr -> mapper.map(currentBounds, currentConstraints, arr));
                    }
                }

                if (mapper.classification() == TypeBound.Classification.BOUND || mapper.classification() == TypeBound.Classification.BOTH) {
                    for (TypeBound.Result.Builder constraint : previousBounds) {
                        if (mapper.accepts(constraint)) {
                            processingBounds.add(constraint);
                        } else {
                            currentBounds.add(constraint);
                        }
                    }

                    if (!processingBounds.isEmpty()) {
                        consumeSubsets(processingBounds, mapper.arity(), arr -> mapper.map(currentBounds, currentConstraints, arr));
                    }
                }


            }
        }
        return Pair.of(currentBounds, currentConstraints);
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
