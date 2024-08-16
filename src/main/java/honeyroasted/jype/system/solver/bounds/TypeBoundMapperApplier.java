package honeyroasted.jype.system.solver.bounds;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class TypeBoundMapperApplier implements TypeBoundMapper {
    private List<TypeBoundMapper> mappers;

    public TypeBoundMapperApplier(List<TypeBoundMapper> mappers) {
        this.mappers = mappers;
    }

    @Override
    public boolean accepts(TypeBound.Result.Builder builder) {
        return true;
    }

    @Override
    public int arity() {
        return -1;
    }

    @Override
    public void map(Context context, TypeBound.Result.Builder... input) {
        List<TypeBound.Result.Builder> constraintSet = new ArrayList<>();
        List<TypeBound.Result.Builder> boundSet = new ArrayList<>();

        if (context.classification() == TypeBound.Classification.BOUND) {
            Collections.addAll(boundSet, input);
        } else {
            Collections.addAll(constraintSet, input);
        }

        Pair<List<TypeBound.Result.Builder>, List<TypeBound.Result.Builder>> result = this.process(context.system(), boundSet, constraintSet);
        addAll(context.bounds(), result.left());
        addAll(context.constraints(), result.right());
    }

    public TypeBound.Result.Builder apply(TypeSystem system, TypeBound bound, TypeBound.Classification classification, TypeBound.Result.Builder... parents) {
        TypeBound.Result.Builder child = TypeBound.Result.builder(bound, parents);
        this.process(system, classification == TypeBound.Classification.BOUND ? List.of(child) : Collections.emptyList(),
                classification == TypeBound.Classification.BOUND ? Collections.emptyList() : List.of(child));
        return child;
    }

    public boolean check(TypeSystem system, TypeBound bound, TypeBound.Classification classification, TypeBound.Result.Builder... parents) {
        return apply(system, bound, classification, parents).satisfied();
    }

    public Pair<List<TypeBound.Result.Builder>, List<TypeBound.Result.Builder>> process(TypeSystem system, List<TypeBound.Result.Builder> inputBounds, List<TypeBound.Result.Builder> inputConstraints) {
        QuadList<TypeBound.Result.Builder> constraints = new QuadList<>(inputConstraints, ArrayList::new);
        QuadList<TypeBound.Result.Builder> bounds = new QuadList<>(inputBounds, ArrayList::new);


        while ((!constraints.compareMatchesCurrent() || !bounds.compareMatchesCurrent())) {
            constraints.stepCompare();
            bounds.stepCompare();

            for (TypeBoundMapper mapper : this.mappers) {
                constraints.stepForwards();
                bounds.stepForwards();

                if (mapper.accepts(TypeBound.Classification.CONSTRAINT)) {
                    applyMapper(system, mapper, TypeBound.Classification.CONSTRAINT, constraints, bounds, constraints);
                } else {
                    constraints.addPreviousToCurrent();
                }

                if (mapper.accepts(TypeBound.Classification.BOUND)) {
                    applyMapper(system, mapper, TypeBound.Classification.BOUND, bounds, bounds, constraints);
                } else {
                    bounds.addPreviousToCurrent();
                }
            }
        }
        return Pair.of(bounds.current(), constraints.current());
    }

    private static void applyMapper(TypeSystem system, TypeBoundMapper mapper, TypeBound.Classification classification, QuadList<TypeBound.Result.Builder> input, QuadList<TypeBound.Result.Builder> bounds, QuadList<TypeBound.Result.Builder> constraints) {
        for (TypeBound.Result.Builder constraint : input.previous()) {
            if (mapper.accepts(constraint)) {
                input.processing().add(constraint);
            } else {
                input.current().add(constraint);
            }
        }

        Set<TypeBound.Result.Builder> consumed = Collections.newSetFromMap(new IdentityHashMap<>());
        consumeSubsets(input.processing(), mapper.arity(), mapper.commutative(), arr -> {
            if (mapper.accepts(arr)) {
                Collections.addAll(consumed, arr);
                mapper.map(new Context(t -> addToBoundList(t, bounds.current()), t -> addToBoundList(t, constraints.current()), bounds.previous(), constraints.previous(), system, classification), arr);
            }
        });

        Set<TypeBound.Result.Builder> ignored = Collections.newSetFromMap(new IdentityHashMap<>());
        ignored.addAll(input.processing());
        ignored.removeAll(consumed);
        input.current().addAll(ignored);
    }

    private static void addToBoundList(TypeBound.Result.Builder builder, Collection<TypeBound.Result.Builder> list) {
        for (TypeBound.Result.Builder curr : list) {
            if (curr == builder) {
                return;
            }
        }

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
            baseCase.accept(processing.toArray(TypeBound.Result.Builder[]::new));
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
            permuteAndConsumeSubset(mem, input, subset, 0, subset.length - 1, baseCase);
        }
    }

    private static void permuteAndConsumeSubset(TypeBound.Result.Builder[] mem, TypeBound.Result.Builder[] input, int[] subset, int l, int h, Consumer<TypeBound.Result.Builder[]> baseCase) {
        if (l == h) {
            copyMem(mem, input, subset);
            baseCase.accept(mem);
        } else {
            for (int i = l; i <= h; i++) {
                swap(subset, l, i);
                permuteAndConsumeSubset(mem, input, subset, l + 1, h, baseCase);
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

    public static class QuadList<T> {
        private List<T> compare;
        private List<T> previous;
        private List<T> processing;
        private List<T> current;

        public QuadList(Collection<T> initial, Supplier<? extends List<T>> factory) {
            this.compare = factory.get();
            this.previous = factory.get();
            this.processing = factory.get();
            this.current = factory.get();

            this.current.addAll(initial);
        }

        public List<T> compare() {
            return this.compare;
        }

        public List<T> previous() {
            return this.previous;
        }

        public List<T> processing() {
            return this.processing;
        }

        public List<T> current() {
            return this.current;
        }

        public void addPreviousToCurrent() {
            this.current.addAll(this.previous);
        }

        public void stepCompare() {
            this.compare.clear();
            this.compare.addAll(this.current);
        }

        public void stepForwards() {
            this.previous.clear();
            this.previous.addAll(this.current);
            this.current.clear();
            this.processing.clear();
        }

        public boolean compareMatchesCurrent() {
            return this.compare.equals(this.current);
        }
    }

}
