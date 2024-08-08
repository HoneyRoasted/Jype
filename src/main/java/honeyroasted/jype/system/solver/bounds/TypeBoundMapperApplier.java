package honeyroasted.jype.system.solver.bounds;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;

import java.util.ArrayList;
import java.util.Collection;
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
    public void map(Context context, TypeBound.Result.Builder... input) {
        List<TypeBound.Result.Builder> constraintSet = new ArrayList<>();
        Collections.addAll(constraintSet, input);

        Pair<List<TypeBound.Result.Builder>, List<TypeBound.Result.Builder>> result = this.process(context.system(), new ArrayList<>(), constraintSet);
        addAll(context.bounds(), result.left());
        addAll(context.constraints(), result.right());
    }

    public Pair<List<TypeBound.Result.Builder>, List<TypeBound.Result.Builder>> process(TypeSystem system, List<TypeBound.Result.Builder> bounds, List<TypeBound.Result.Builder> constraints) {
        List<TypeBound.Result.Builder> compareConstraints = new ArrayList<>();
        List<TypeBound.Result.Builder> previousConstraints = new ArrayList<>();
        List<TypeBound.Result.Builder> processingConstraints = new ArrayList<>();
        List<TypeBound.Result.Builder> currentConstraints = new ArrayList<>(constraints);

        List<TypeBound.Result.Builder> compareBounds = new ArrayList<>();
        List<TypeBound.Result.Builder> previousBounds = new ArrayList<>();
        List<TypeBound.Result.Builder> processingBounds = new ArrayList<>();
        List<TypeBound.Result.Builder> currentBounds = new ArrayList<>(bounds);


        while (!compareConstraints.equals(currentConstraints) || !compareBounds.equals(currentBounds)) {
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
                        consumeSubsets(processingConstraints, mapper.arity(), mapper.commutative(), arr -> {
                            if (mapper.accepts(arr)) {
                                mapper.map(new Context(t -> addToBoundList(t, currentBounds), t -> addToBoundList(t, currentConstraints),
                                        previousBounds, previousConstraints, system, TypeBound.Classification.CONSTRAINT), arr);
                            }
                        });
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
                        consumeSubsets(processingBounds, mapper.arity(), mapper.commutative(), arr -> {
                            if (mapper.accepts(arr)) {
                                mapper.map(new Context(t -> addToBoundList(t, currentBounds), t -> addToBoundList(t, currentConstraints),
                                        previousBounds, previousConstraints, system, TypeBound.Classification.BOUND), arr);
                            }
                        });
                    }
                }


            }
        }
        return Pair.of(currentBounds, currentConstraints);
    }

    private static void addToBoundList(TypeBound.Result.Builder builder, Collection<TypeBound.Result.Builder> list) {
        for (TypeBound.Result.Builder curr : list) {
            if (curr.bound().equals(builder.bound())) {
                builder.replaceWith(curr);
                return;
            }
        }

        list.add(builder);
    }

    private static void consumeSubsets(List<TypeBound.Result.Builder> processing, int size, boolean commutative, Consumer<TypeBound.Result.Builder[]> baseCase) {
        if (size <= 0 || size == processing.size()) {
            baseCase.accept(processing.toArray(new TypeBound.Result.Builder[0]));
        } else if (size < processing.size()) {
            TypeBound.Result.Builder[] mem = new TypeBound.Result.Builder[size];
            TypeBound.Result.Builder[] input = processing.toArray(TypeBound.Result.Builder[]::new);
            int[] subset = IntStream.range(0, size).toArray();

            consumeSubset(mem, input, subset, commutative, baseCase);
            while (true) {
                int i;
                for (i = size - 1; i >= 0 && subset[i] == input.length - size + i; i--) ;
                if (i < 0) break;

                subset[i]++;
                for (++i; i < size; i++) {
                    subset[i] = subset[i - 1] + 1;
                }
                consumeSubset(mem, input, subset, commutative, baseCase);
            }
        }
    }

    private static void consumeSubset(TypeBound.Result.Builder[] mem, TypeBound.Result.Builder[] input, int[] subset, boolean commutative, Consumer<TypeBound.Result.Builder[]> baseCase) {
        if (commutative) {
            copyMem(mem, input, subset);
            baseCase.accept(mem);
        } else {
            permuteSubset(mem, input, subset, 0, subset.length - 1, baseCase);
        }
    }

    private static void permuteSubset(TypeBound.Result.Builder[] mem, TypeBound.Result.Builder[] input, int[] subset, int l, int h, Consumer<TypeBound.Result.Builder[]> baseCase) {
        if (l == h) {
            copyMem(mem, input, subset);
            baseCase.accept(mem);
        } else {
            for (int i = l; i <= h; i++) {
                swap(subset, l, i);
                permuteSubset(mem, input, subset, l + 1, h, baseCase);
                swap(subset, l, i);
            }
        }
    }

   private static void swap(int nums[], int l, int i) {
        int temp = nums[l];
        nums[l] = nums[i];
        nums[i] = temp;
    }


    private static void copyMem(TypeBound.Result.Builder[] mem, TypeBound.Result.Builder[] input, int[] subset) {
        for (int i = 0; i < subset.length; i++) {
            mem[i] = input[subset[i]];
        }
    }

}
