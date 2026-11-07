package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.JFreshTypeVariables;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JMetaVarTypeResolveVisitor;
import honeyroasted.jype.system.visitor.visitors.JRecursiveTypeVisitor;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JResolveBounds extends ConstraintMapper.All {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch) {
        return branch.status().isTrue();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch) {
        JTypeSystem system = branch.parent().metadata().firstOr(JTypeSystem.class, JTypeSystem.RUNTIME_REFLECTION);

        Set<JMetaVarType> variables = mentionedVariables(branch);
        Map<JMetaVarType, JType> instantiations = new LinkedHashMap<>();
        for (JMetaVarType mvt : variables) {
            JType inst = findInstantiation(mvt, branch);
            if (inst != null) {
                instantiations.put(mvt, inst);
            }
        }

        if (instantiations.size() == variables.size()) {
            // JLS §18.4 every variable in V has an instantiation
            instantiations.forEach((mvt, t) -> recordInstantiation(branch, mvt, t));
            return;
        }

        Map<JMetaVarType, Set<JMetaVarType>> dependencies = discoverDependencies(branch, variables);
        List<JMetaVarType> subset = findSubset(variables, dependencies, instantiations);
        if (subset.isEmpty()) {
            branch.add(Constraint.FALSE, Constraint.Status.FALSE);
            return;
        }

        boolean hasCapture = subset.stream().anyMatch(mvt -> captureBounds(branch).stream().anyMatch(cpt -> cpt.left().typeArguments().contains(mvt)));

        if (!hasCapture) {
            Map<JMetaVarType, JType> candidates = new LinkedHashMap<>();
            for (JMetaVarType mvt : subset) {
                Set<JType> lower = properLowerBounds(branch, mvt);
                Set<JType> upper = properUpperBounds(branch, mvt);
                if (!lower.isEmpty()) {
                    // JLS §18.4 Ti = lub(L1...Lk)
                    candidates.put(mvt, system.operations().findLeastUpperBound(lower));
                } else if (hasThrowsBound(branch, mvt) && upper.stream().allMatch(u -> system.operations().isSubtype(system.constants().runtimeException(), u))) {
                    // JLS §18.4 throws αi with every proper upper bound a supertype of RuntimeException
                    candidates.put(mvt, system.constants().runtimeException());
                } else {
                    // JLS §18.4 Ti = glb(U1...Uk)
                    if (upper.isEmpty()) {
                        upper = Set.of(system.constants().object());
                    }
                    candidates.put(mvt, system.operations().findGreatestLowerBound(upper));
                }
            }

            Optional<ConstraintBranch> result = incorporate(system, branch, candidates, List.of());
            if (result.isPresent()) {
                merge(branch, result.get(), candidates, List.of());
                return;
            }
        }

        // JLS §18.4 fresh type variables Y1...Yn
        Map<JMetaVarType, JVarType> fresh = new LinkedHashMap<>();
        for (JMetaVarType mvt : subset) {
            fresh.put(mvt, JFreshTypeVariables.create(system, "Y"));
        }
        Map<JMetaVarType, JType> theta = new HashMap<>(instantiations);
        theta.putAll(fresh);
        JMetaVarTypeResolveVisitor thetaVisitor = new JMetaVarTypeResolveVisitor(theta);

        for (JMetaVarType mvt : subset) {
            Set<JType> lower = properLowerBounds(branch, mvt);
            Set<JType> upper = new LinkedHashSet<>();
            for (JType u : allUpperBounds(branch, mvt)) {
                upper.add(thetaVisitor.visit(u));
            }
            if (upper.isEmpty()) {
                upper.add(system.constants().object());
            }
            JType glb = system.operations().findGreatestLowerBound(upper);
            Set<JType> lowerSet = lower.isEmpty() ? Set.of() : Set.of(system.operations().findLeastUpperBound(lower));
            JFreshTypeVariables.finish(fresh.get(mvt), Set.of(glb), lowerSet);

            // JLS §18.4 Yi must have well-formed bounds
            for (JType l : lowerSet) {
                if (!system.operations().isSubtype(l, glb)) {
                    branch.add(Constraint.FALSE, Constraint.Status.FALSE);
                    return;
                }
            }
        }

        List<JTypeConstraints.Capture> removed = captureBounds(branch).stream()
                .filter(cpt -> cpt.left().typeArguments().stream().anyMatch(subset::contains)).toList();
        Map<JMetaVarType, JType> freshCandidates = new LinkedHashMap<>(fresh);
        Optional<ConstraintBranch> result = incorporate(system, branch, freshCandidates, removed);
        if (result.isPresent()) {
            merge(branch, result.get(), freshCandidates, removed);
        } else {
            branch.add(Constraint.FALSE, Constraint.Status.FALSE);
        }
    }

    private static Optional<ConstraintBranch> incorporate(JTypeSystem system, ConstraintBranch branch, Map<JMetaVarType, JType> candidates, List<JTypeConstraints.Capture> removed) {
        ConstraintTree temp = new ConstraintTree();
        temp.metadata().inheritFrom(branch.parent().metadata());
        ConstraintBranch snap = branch.copy(temp);
        snap.executeChanges();
        temp.addBranch(snap);
        removed.forEach(snap::drop);
        candidates.forEach((mvt, t) -> snap.add(new JTypeConstraints.Equal(mvt, t), Constraint.Status.TRUE));
        snap.executeChanges();

        system.operations().incorporationApplier().accept(temp);
        system.operations().reductionApplier().accept(temp);

        return temp.branches().stream().filter(b -> b.status().isTrue()).findFirst();
    }

    private static void merge(ConstraintBranch branch, ConstraintBranch result, Map<JMetaVarType, JType> candidates, List<JTypeConstraints.Capture> removed) {
        removed.forEach(branch::drop);
        Set<Constraint> removedSet = new LinkedHashSet<>(removed);
        result.constraints().forEach((con, status) -> {
            if (!removedSet.contains(con)) {
                if (branch.constraints().containsKey(con)) {
                    if (branch.status(con) != status) {
                        branch.set(con, status);
                    }
                } else {
                    branch.add(con, status);
                }
            }
        });
        candidates.forEach((mvt, t) -> recordInstantiation(branch, mvt, t));
        branch.metadata().inheritFrom(result.metadata());
    }

    private static void recordInstantiation(ConstraintBranch branch, JMetaVarType mvt, JType t) {
        branch.add(new JTypeConstraints.Instantiation(mvt, t), Constraint.Status.TRUE);
        branch.metadata().firstOrAttach(JTypeContext.TypeMetavarMap.class, JTypeContext.TypeMetavarMap.createEmpty())
                .instantiations().put(mvt, t);
    }

    private static boolean hasThrowsBound(ConstraintBranch branch, JMetaVarType mvt) {
        return branch.constraints().entrySet().stream().anyMatch(e -> e.getValue().isTrue() && e.getKey() instanceof JTypeConstraints.Throws thr && thr.value().typeEquals(mvt));
    }

    private static List<JTypeConstraints.Capture> captureBounds(ConstraintBranch branch) {
        List<JTypeConstraints.Capture> result = new ArrayList<>();
        branch.constraints().forEach((con, status) -> {
            if (status.isTrue() && con instanceof JTypeConstraints.Capture cpt) {
                result.add(cpt);
            }
        });
        return result;
    }

    private static Set<JType> properLowerBounds(ConstraintBranch branch, JMetaVarType mvt) {
        Set<JType> result = new LinkedHashSet<>();
        branch.constraints().forEach((con, status) -> {
            if (status.isTrue() && con instanceof JTypeConstraints.Subtype st && st.right().typeEquals(mvt) && st.left().isProperType() && !st.left().isNullType()) {
                result.add(st.left());
            }
        });
        return result;
    }

    private static Set<JType> properUpperBounds(ConstraintBranch branch, JMetaVarType mvt) {
        Set<JType> result = new LinkedHashSet<>();
        for (JType t : allUpperBounds(branch, mvt)) {
            if (t.isProperType()) {
                result.add(t);
            }
        }
        return result;
    }

    private static Set<JType> allUpperBounds(ConstraintBranch branch, JMetaVarType mvt) {
        Set<JType> result = new LinkedHashSet<>();
        branch.constraints().forEach((con, status) -> {
            if (status.isTrue() && con instanceof JTypeConstraints.Subtype st && st.left().typeEquals(mvt) && !st.right().typeEquals(mvt)) {
                result.add(st.right());
            }
        });
        return result;
    }

    private static List<JMetaVarType> findSubset(Set<JMetaVarType> variables, Map<JMetaVarType, Set<JMetaVarType>> dependencies, Map<JMetaVarType, JType> instantiations) {
        List<Set<JMetaVarType>> components = stronglyConnected(variables, dependencies, instantiations);
        for (Set<JMetaVarType> component : components) {
            boolean ready = true;
            for (JMetaVarType mvt : component) {
                for (JMetaVarType dep : dependencies.getOrDefault(mvt, Set.of())) {
                    if (!component.contains(dep) && !instantiations.containsKey(dep)) {
                        ready = false;
                        break;
                    }
                }
                if (!ready) {
                    break;
                }
            }
            if (ready) {
                return new ArrayList<>(component);
            }
        }
        return List.of();
    }

    private static List<Set<JMetaVarType>> stronglyConnected(Set<JMetaVarType> variables, Map<JMetaVarType, Set<JMetaVarType>> dependencies, Map<JMetaVarType, JType> instantiations) {
        List<JMetaVarType> nodes = variables.stream().filter(v -> !instantiations.containsKey(v)).toList();
        Map<JMetaVarType, Integer> index = new HashMap<>();
        Map<JMetaVarType, Integer> low = new HashMap<>();
        Deque<JMetaVarType> stack = new ArrayDeque<>();
        Set<JMetaVarType> onStack = new LinkedHashSet<>();
        List<Set<JMetaVarType>> result = new ArrayList<>();
        int[] counter = {0};
        for (JMetaVarType node : nodes) {
            if (!index.containsKey(node)) {
                strongConnect(node, nodes, dependencies, index, low, stack, onStack, result, counter);
            }
        }
        return result;
    }

    private static void strongConnect(JMetaVarType v, List<JMetaVarType> nodes, Map<JMetaVarType, Set<JMetaVarType>> dependencies, Map<JMetaVarType, Integer> index, Map<JMetaVarType, Integer> low, Deque<JMetaVarType> stack, Set<JMetaVarType> onStack, List<Set<JMetaVarType>> result, int[] counter) {
        index.put(v, counter[0]);
        low.put(v, counter[0]);
        counter[0]++;
        stack.push(v);
        onStack.add(v);
        for (JMetaVarType w : dependencies.getOrDefault(v, Set.of())) {
            if (!nodes.contains(w)) {
                continue;
            }
            if (!index.containsKey(w)) {
                strongConnect(w, nodes, dependencies, index, low, stack, onStack, result, counter);
                low.put(v, Math.min(low.get(v), low.get(w)));
            } else if (onStack.contains(w)) {
                low.put(v, Math.min(low.get(v), index.get(w)));
            }
        }
        if (low.get(v).equals(index.get(v))) {
            Set<JMetaVarType> component = new LinkedHashSet<>();
            JMetaVarType w;
            do {
                w = stack.pop();
                onStack.remove(w);
                component.add(w);
            } while (!w.equals(v));
            result.add(component);
        }
    }

    public static JType findInstantiation(JMetaVarType mvt, ConstraintBranch bounds) {
        Set<JType> types = new LinkedHashSet<>();
        bounds.constraints().forEach((bound, status) -> {
            if (status.isTrue()) {
                if (bound instanceof JTypeConstraints.Equal eq) {
                    if (eq.left().typeEquals(mvt) && eq.right().isProperType()) {
                        types.add(eq.right());
                    } else if (eq.right().typeEquals(mvt) && eq.left().isProperType()) {
                        types.add(eq.left());
                    }
                } else if (bound instanceof JTypeConstraints.Instantiation inst) {
                    if (inst.left().typeEquals(mvt)) {
                        types.add(inst.right());
                    }
                }
            }
        });
        return types.isEmpty() ? null : mvt.typeSystem().operations().findMostSpecificType(types);
    }

    private static Set<JMetaVarType> mentionedVariables(ConstraintBranch branch) {
        Set<JMetaVarType> result = new LinkedHashSet<>();
        branch.constraints().forEach((con, status) -> {
            if (status.isTrue() && isBound(con)) {
                Constraint.Binary<?, ?> bin = (Constraint.Binary<?, ?>) con;
                if (bin.left() instanceof JType lt) {
                    result.addAll(discoverMetaVars(lt));
                }
                if (bin.right() instanceof JType rt) {
                    result.addAll(discoverMetaVars(rt));
                }
            } else if (status.isTrue() && con instanceof JTypeConstraints.Throws thr) {
                result.add(thr.value());
            } else if (con instanceof JTypeConstraints.Infer inf) {
                result.add(inf.left());
            }
        });
        return result;
    }

    private static boolean isBound(Constraint con) {
        return con instanceof JTypeConstraints.Equal || con instanceof JTypeConstraints.Subtype || con instanceof JTypeConstraints.Capture;
    }

    private static Map<JMetaVarType, Set<JMetaVarType>> discoverDependencies(ConstraintBranch branch, Set<JMetaVarType> variables) {
        Map<JMetaVarType, Set<JMetaVarType>> dependencies = new LinkedHashMap<>();
        variables.forEach(v -> dependencies.computeIfAbsent(v, k -> new LinkedHashSet<>()).add(v));

        Set<JMetaVarType> captured = new LinkedHashSet<>();
        List<JTypeConstraints.Capture> captures = captureBounds(branch);
        for (JTypeConstraints.Capture cpt : captures) {
            for (JType arg : cpt.left().typeArguments()) {
                if (arg instanceof JMetaVarType mvt) {
                    captured.add(mvt);
                }
            }
        }

        for (JTypeConstraints.Capture cpt : captures) {
            Set<JMetaVarType> mentioned = discoverMetaVars(cpt.left());
            mentioned.addAll(discoverMetaVars(cpt.right()));
            for (JType arg : cpt.left().typeArguments()) {
                if (arg instanceof JMetaVarType alpha) {
                    // JLS §18.4 α on the left of a capture bound depends on every variable in it
                    dependencies.computeIfAbsent(alpha, k -> new LinkedHashSet<>()).addAll(mentioned);
                }
            }
        }

        branch.constraints().forEach((con, status) -> {
            if (status.isTrue() && (con instanceof JTypeConstraints.Equal || con instanceof JTypeConstraints.Subtype)) {
                Constraint.Binary<JType, JType> bin = (Constraint.Binary<JType, JType>) con;
                addDependency(dependencies, captured, bin.left(), bin.right());
                addDependency(dependencies, captured, bin.right(), bin.left());
            }
        });

        boolean changed;
        do {
            changed = false;
            for (Map.Entry<JMetaVarType, Set<JMetaVarType>> entry : dependencies.entrySet()) {
                Set<JMetaVarType> closure = new LinkedHashSet<>(entry.getValue());
                for (JMetaVarType dep : entry.getValue()) {
                    closure.addAll(dependencies.getOrDefault(dep, Set.of()));
                }
                if (closure.size() != entry.getValue().size()) {
                    entry.setValue(closure);
                    changed = true;
                }
            }
        } while (changed);

        return dependencies;
    }

    private static void addDependency(Map<JMetaVarType, Set<JMetaVarType>> dependencies, Set<JMetaVarType> captured, JType side, JType other) {
        if (side instanceof JMetaVarType alpha) {
            for (JMetaVarType beta : discoverMetaVars(other)) {
                if (captured.contains(alpha)) {
                    // JLS §18.4 α captured: β depends on α
                    dependencies.computeIfAbsent(beta, k -> new LinkedHashSet<>()).add(alpha);
                } else {
                    // JLS §18.4 α depends on β
                    dependencies.computeIfAbsent(alpha, k -> new LinkedHashSet<>()).add(beta);
                }
            }
        }
    }

    private static Set<JMetaVarType> discoverMetaVars(JType visit) {
        return new LinkedHashSet<>(new JRecursiveTypeVisitor<JMetaVarType, Void>((JTypeVisitor.Default) (type, context) -> type instanceof JMetaVarType mvt ? mvt : null,
                null, false).visit(visit, new HashMap<>()));
    }
}
