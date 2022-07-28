package honeyroasted.jype.system.solver.impl;

import honeyroasted.jype.Type;
import honeyroasted.jype.TypeConcrete;
import honeyroasted.jype.system.TypeConstraint;
import honeyroasted.jype.system.solver.TypeContext;
import honeyroasted.jype.system.solver.TypeSolution;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.type.TypeOr;
import honeyroasted.jype.type.TypeParameter;
import org.paukov.combinatorics3.Generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BruteForceTypeSolver implements TypeSolver {
    private List<TypeConstraint> constraints = new ArrayList<>();
    private Predicate<Type> consider;

    public BruteForceTypeSolver(Predicate<Type> consider) {
        this.consider = consider;
    }

    public TypeSolver constrain(TypeConstraint constraint) {
        this.constraints.add(constraint);
        return this;
    }

    public TypeSolution solve() {
        TypeConstraint.And root = new TypeConstraint.And(this.constraints);
        TypeContext context = new TypeContext();

        return new TypeSolution(context, root, List.of(), false);
    }

    private List<TypeContext> allCombinations(TypeConstraint.Or or) {
        Map<TypeParameter, List<TypeConcrete>> possibilities = new HashMap<>();

        for (TypeConstraint constraint : or.constraints()) {
            if (constraint instanceof TypeConstraint.Bound bound && bound.kind() == TypeConstraint.Bound.Kind.VAR_TO_BOUND) {
                if (this.consider.test(bound.subtype())) {
                    possibilities.computeIfAbsent((TypeParameter) bound.subtype(), key -> new ArrayList<>()).add(bound.parent());
                }
            }
        }

        Map<TypeParameter, List<List<TypeConcrete>>> allPossibilities = new HashMap<>();
        possibilities.forEach((type, list) -> allPossibilities.put(type,
                Generator.subset(list).simple().stream().filter(ls -> !ls.isEmpty()).toList()));

        List<Map<TypeParameter, List<TypeConcrete>>> allContexts = new ArrayList<>();
        combinationsHelper(allPossibilities, allPossibilities.keySet().stream().findFirst().get(), new HashSet<>(), new HashMap<>(), allContexts);
        return allContexts.stream().map(map -> {
            TypeContext context = new TypeContext();
            map.forEach((param, list) -> context.put(param, new TypeOr(list)));
            return context;
        }).toList();
    }

    private static void combinationsHelper(Map<TypeParameter, List<List<TypeConcrete>>> allPossibilities, TypeParameter current, Set<TypeParameter> walked, Map<TypeParameter, List<TypeConcrete>> building, List<Map<TypeParameter, List<TypeConcrete>>> res) {
        if (walked.containsAll(allPossibilities.keySet())) {
            res.add(building);
        } else {
            Set<TypeParameter> newWalked = new HashSet<>(walked);
            newWalked.add(current);

            allPossibilities.get(current).forEach(list -> {
                Map<TypeParameter, List<TypeConcrete>> newBuilding = new HashMap<>(building);
                newBuilding.put(current, list);
                allPossibilities.keySet().stream().filter(p -> !newWalked.contains(p)).forEach(param -> {
                    combinationsHelper(allPossibilities, param, newWalked, newBuilding, res);
                });
            });
        }
    }

    private <T> List<T> append(List<T>... lists) {
        List<T> list = new ArrayList<>();
        for (List<T> ts : lists) {
            list.addAll(ts);
        }
        return list;
    }

    private static void walk(TypeConstraint constraint, Consumer<TypeConstraint> consumer) {
        consumer.accept(constraint);

        if (constraint instanceof TypeConstraint.And and) {
            and.constraints().forEach(t -> walk(t, consumer));
        } else if (constraint instanceof TypeConstraint.Or or) {
            or.constraints().forEach(t -> walk(t, consumer));
        }
    }

}
