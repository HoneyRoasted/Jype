package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.collect.multi.Pair;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.solver.constraints.JTypeContext;
import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JMetaVarTypeResolver;
import honeyroasted.jype.system.visitor.visitors.JRecursiveTypeVisitor;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JResolveBounds extends ConstraintMapper.All {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch) {
        return branch.status().isTrue();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch) {
        PropertySet instanceContext = branch.parent().metadata();
        Function<JType, JType> mapper = instanceContext.firstOr(JTypeContext.JTypeMapper.class, JTypeContext.JTypeMapper.NO_OP).mapper().apply(branch);

        JTypeSystem system = instanceContext.firstOr(JTypeSystem.class, JTypeSystem.RUNTIME_REFLECTION);

        Map<JMetaVarType, Set<JMetaVarType>> dependencies = this.discoverDependencies(branch, mapper);

        Set<JMetaVarType> varsAndDeps = new LinkedHashSet<>();
        dependencies.forEach((mvt, deps) -> {
            varsAndDeps.add(mvt);
            varsAndDeps.addAll(deps);
        });

        Map<JMetaVarType, JType> instantiations = findAllInstantiations(varsAndDeps, branch);
        boolean foundAllInstantiations = instantiations.size() == varsAndDeps.size();

        if (foundAllInstantiations) {
            instantiations.forEach((mvt, t) -> branch.add(new JTypeConstraints.Instantiation(mvt, t), Constraint.Status.TRUE));
            return;
        }

        Set<JMetaVarType> subset = findSubset(varsAndDeps, dependencies, instantiations);
        if (!subset.isEmpty()) {
            Map<Constraint, Constraint.Status> generatedBounds = new HashMap<>();

            //Bound set does not contain any bound of the form G<..., a_i, ...> = capture(G<...>)
            boolean hasCapture = branch.constraints().entrySet().stream().anyMatch(node -> node.getValue().isTrue() && node.getKey() instanceof JTypeConstraints.Capture cpt &&
                    ((JClassType) mapper.apply(cpt.left())).typeArguments().stream().anyMatch(subset::contains));

            if (!hasCapture) {
                Map<JMetaVarType, Pair<JType, Constraint>> candidates = new LinkedHashMap<>();
                boolean foundAllCandidates = true;
                for (JMetaVarType mvt : subset) {
                    Pair<Map<JType, Constraint>, Map<JType, Constraint>> properBounds = this.findProperBounds(mvt, branch.constraints(), mapper);
                    Map<JType, Constraint> properUpper = properBounds.left();
                    Map<JType, Constraint> properLower = properBounds.right();

                    if (!properLower.isEmpty()) {
                        JType lub = mvt.typeSystem().operations().findLeastUpperBound(properLower.keySet());
                        candidates.put(mvt, Pair.of(lub, new JTypeConstraints.Equal(mvt, lub)));
                    } else if (branch.constraints().entrySet().stream().anyMatch(cn -> cn.getValue().isTrue() && cn.getKey() instanceof JTypeConstraints.Throws thr && mapper.apply(thr.value()).equals(mvt)) &&
                            properUpper.keySet().stream().allMatch(t -> system.operations().isSubtype(t, mvt.typeSystem().constants().runtimeException()))) {
                        JType cand = mvt.typeSystem().constants().runtimeException();
                        candidates.put(mvt, Pair.of(cand, new JTypeConstraints.Equal(mvt, cand)));
                    } else if (!properUpper.isEmpty()) {
                        JType glb = mvt.typeSystem().operations().findGreatestLowerBound(properUpper.keySet());
                        candidates.put(mvt, Pair.of(glb, new JTypeConstraints.Equal(mvt, glb)));
                    } else {
                        foundAllCandidates = false;
                    }
                }

                if (foundAllCandidates) {
                    ConstraintTree temp = new ConstraintTree();
                    ConstraintBranch snap = branch.copy(temp);
                    temp.addBranch(snap);

                    candidates.forEach((k, v) -> {
                        snap.add(v.right(), Constraint.Status.TRUE);
                        instantiations.put(k, v.left());
                    });

                    system.operations().incorporationApplier().accept(temp);
                    system.operations().reductionApplier().accept(temp);
                    generatedBounds.putAll(snap.constraints());
                }
            }

            if (hasCapture || generatedBounds.isEmpty() || generatedBounds.entrySet().stream().anyMatch(e -> e.getKey() instanceof False || e.getValue() == Constraint.Status.FALSE)) {
                Map<JMetaVarType, JMetaVarType> freshVars = new LinkedHashMap<>();
                varsAndDeps.forEach(mv -> freshVars.put(mv, mv.typeSystem().typeFactory().newMetaVarType(mv.name() + "_y")));
                JMetaVarTypeResolver theta = new JMetaVarTypeResolver(freshVars);

                Map<Constraint, Constraint.Status> newBounds = new HashMap<>();
                branch.constraints().forEach((con, stat) -> {
                    if (stat.isTrue()) {
                        newBounds.put(con, stat);
                    }
                });

                for (JMetaVarType mvt : varsAndDeps) {
                    JMetaVarType y = freshVars.get(mvt);

                    Pair<Map<JType, Constraint>, Map<JType, Constraint>> properBounds = this.findProperBounds(mvt, branch.constraints(), mapper);
                    Map<JType, Constraint> properUpper = properBounds.left();
                    Map<JType, Constraint> properLower = properBounds.right();

                    if (!properLower.isEmpty()) {
                        JType lower = mvt.typeSystem().operations().findLeastUpperBound(properLower.keySet());
                        newBounds.put(new JTypeConstraints.Equal(mvt, lower), Constraint.Status.ASSUMED);
                        y.lowerBounds().add(lower);
                    }

                    if (!properUpper.isEmpty()) {
                        JType upper = mvt.typeSystem().operations().findGreatestLowerBound(properUpper.keySet().stream().map(theta).collect(Collectors.toCollection(LinkedHashSet::new)));
                        newBounds.put(new JTypeConstraints.Equal(mvt, upper), Constraint.Status.ASSUMED);
                        y.upperBounds().add(upper);
                    }
                }

                newBounds.entrySet().removeIf(entry -> entry.getKey() instanceof JTypeConstraints.Capture cpt && cpt.left().typeArguments().stream().anyMatch(subset::contains));

                freshVars.forEach((mv, fresh) -> newBounds.put(new JTypeConstraints.Equal(mv, fresh), Constraint.Status.ASSUMED));

                generatedBounds.clear();
                generatedBounds.putAll(newBounds);
            }

            ConstraintTree temp = new ConstraintTree();
            ConstraintBranch snap = new ConstraintBranch(temp);
            temp.addBranch(snap);
            generatedBounds.forEach(snap::add);

            system.operations().incorporationApplier().accept(temp);
            system.operations().reductionApplier().accept(temp);

            if (temp.numBranches() == 1 && snap.status().isTrue()) {
                //Diverging means it failed, I think
                branch.metadata().inheritFrom(snap.metadata());
                snap.constraints().forEach(branch::add);
            } else {
                branch.add(Constraint.FALSE, Constraint.Status.FALSE);
            }
        } else {
            branch.add(Constraint.FALSE, Constraint.Status.FALSE);
        }
    }

    private Pair<Map<JType, Constraint>, Map<JType, Constraint>> findProperBounds(JMetaVarType mvt, Map<Constraint, Constraint.Status> bounds, Function<JType, JType> mapper) {
        Map<JType, Constraint> upperBounds = new LinkedHashMap<>();
        Map<JType, Constraint> lowerBounds = new LinkedHashMap<>();

        bounds.forEach((bound, status) -> {
            if (status.isTrue()) {
                if (bound instanceof JTypeConstraints.Subtype st) {
                    JType left = mapper.apply(st.left());
                    JType right = mapper.apply(st.right());

                    if (left.equals(mvt) && right.isProperType()) {
                        upperBounds.put(right, bound);
                    }

                    if (right.equals(mvt) && left.isProperType()) {
                        lowerBounds.put(left, bound);
                    }
                }
            }
        });

        return Pair.of(upperBounds, lowerBounds);
    }

    private Set<JMetaVarType> findSubset(Set<JMetaVarType> currentMetaVars, Map<JMetaVarType, Set<JMetaVarType>> dependencies, Map<JMetaVarType, JType> instantiations) {
        for (JMetaVarType mvt : currentMetaVars) {
            Set<JMetaVarType> subset = trySubsetWithBase(mvt, dependencies, instantiations, Collections.emptySet());
            if (!subset.isEmpty()) {
                return subset;
            }
        }

        return Collections.emptySet();
    }

    private Set<JMetaVarType> trySubsetWithBase(JMetaVarType base, Map<JMetaVarType, Set<JMetaVarType>> dependencies, Map<JMetaVarType, JType> instantiations, Set<JMetaVarType> building) {
        if (building.contains(base)) return building;

        Set<JMetaVarType> res = new HashSet<>(building);
        res.add(base);

        for (JMetaVarType dep : dependencies.get(base)) {
            if (base != dep) {
                if (instantiations.containsKey(dep)) {
                    res.add(dep);
                } else {
                    boolean foundEquiv = true;
                    for (JType eq : dep.equalities()) {
                        if (eq instanceof JMetaVarType equiv) {
                            Set<JMetaVarType> discover = trySubsetWithBase(equiv, dependencies, instantiations, res);
                            if (!discover.isEmpty()) {
                                res.addAll(discover);
                                break;
                            } else {
                                foundEquiv = false;
                            }
                        }
                    }

                    if (!foundEquiv) {
                        return Collections.emptySet();
                    }
                }
            }
        }

        return res;
    }

    private Map<JMetaVarType, JType> findAllInstantiations(Set<JMetaVarType> mvts, ConstraintBranch bounds) {
        Map<JMetaVarType, JType> instantiations = new LinkedHashMap<>();
        for (JMetaVarType mvt : mvts) {
            JType instantiation = instantiations.getOrDefault(mvt, findInstantiation(mvt, bounds));
            if (instantiation == null) {
                return new LinkedHashMap<>();
            } else {
                instantiations.put(mvt, instantiation);
            }
        }

        return instantiations;
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

    public Map<JMetaVarType, Set<JMetaVarType>> discoverDependencies(ConstraintBranch bounds, Function<JType, JType> mapper) {
        Map<JMetaVarType, Set<JMetaVarType>> dependencies = new LinkedHashMap<>();
        bounds.constraints().forEach((bound, status) -> {
            if (bound instanceof Constraint.Trinary<?, ?, ?> tri) {
                if (tri.left() instanceof JType lt) {
                    initDependencies(mapper.apply(lt), dependencies);
                }

                if (tri.middle() instanceof JType mt) {
                    initDependencies(mapper.apply(mt), dependencies);
                }

                if (tri.right() instanceof JType rt) {
                    initDependencies(mapper.apply(rt), dependencies);
                }
            } else if (bound instanceof Constraint.Binary<?, ?> bin) {
                if (bin.left() instanceof JType lt) {
                    initDependencies(mapper.apply(lt), dependencies);
                }

                if (bin.right() instanceof JType rt) {
                    initDependencies(mapper.apply(rt), dependencies);
                }
            } else if (bound instanceof Constraint.Unary<?> un && un.value() instanceof JType t) {
                initDependencies(mapper.apply(t), dependencies);
            }
        });

        bounds.constraints().forEach((bound, status) -> {
            if (status.isTrue()) {
                if (bound instanceof JTypeConstraints.Equal || bound instanceof JTypeConstraints.Subtype) {
                    Constraint.Binary<JType, JType> bin = (Constraint.Binary<JType, JType>) bound;
                    if (mapper.apply(bin.left()) instanceof JMetaVarType || mapper.apply(bin.right()) instanceof JMetaVarType) {
                        JMetaVarType mvt = (JMetaVarType) mapper.apply(bin.left() instanceof JMetaVarType ? bin.left() : bin.right());
                        JType otherType = mapper.apply(bin.left() instanceof JMetaVarType ? bin.right() : bin.left());

                        boolean foundInCapture = false;
                        for (Map.Entry<Constraint, Constraint.Status> entry : bounds.constraints().entrySet()) {
                            Constraint other = entry.getKey();
                            Constraint.Status otherStatus = entry.getValue();
                            if (otherStatus.isTrue() && other != bound) {
                                if (other instanceof JTypeConstraints.Capture capture) {
                                    if (discoverMetaVars(mapper.apply(capture.left())).contains(mvt)) {
                                        foundInCapture = true;
                                        break;
                                    }
                                }
                            }
                        }

                        JMetaVarType alpha = mvt;
                        if (foundInCapture) {
                            discoverMetaVars(otherType).forEach(beta -> dependencies.get(beta).add(alpha));
                        } else {
                            discoverMetaVars(otherType).forEach(beta -> dependencies.get(alpha).add(beta));
                        }
                    }
                } else if (bound instanceof JTypeConstraints.Capture capture) {
                    Set<JMetaVarType> mvts = discoverMetaVars(mapper.apply(capture.left()));
                    mvts.addAll(discoverMetaVars(mapper.apply(capture.right())));
                    mvts.forEach(mvt -> dependencies.get(mvt).addAll(mvts));
                }
            }
        });

        Map<JMetaVarType, Set<JMetaVarType>> previous = dependencies;
        Map<JMetaVarType, Set<JMetaVarType>> current;
        do {
            current = new LinkedHashMap<>();

            Map<JMetaVarType, Set<JMetaVarType>> finalCurrent = current;
            Map<JMetaVarType, Set<JMetaVarType>> finalPrevious = previous;
            previous.forEach((mvt, deps) -> {
                Set<JMetaVarType> newDeps = new LinkedHashSet<>(deps);
                deps.forEach(dep -> {
                    if (dep != mvt) {
                        newDeps.addAll(finalPrevious.get(dep));
                    }
                });
                finalCurrent.put(mvt, newDeps);
            });
            previous = current;
        } while (!current.equals(previous));

        return dependencies;
    }

    private static void initDependencies(JType visit, Map<JMetaVarType, Set<JMetaVarType>> dependencies) {
        discoverMetaVars(visit).forEach(mv -> dependencies.computeIfAbsent(mv, k -> {
            Set<JMetaVarType> deps = new LinkedHashSet<>();
            deps.add(k);
            return deps;
        }));
    }

    private static Set<JMetaVarType> discoverMetaVars(JType visit) {
        return new HashSet<>(new JRecursiveTypeVisitor<JMetaVarType, Void>((JTypeVisitor.Default) (type, context) -> type instanceof JMetaVarType mvt ? mvt : null,
                null, false).visit(visit, new HashMap<>()));
    }
}
