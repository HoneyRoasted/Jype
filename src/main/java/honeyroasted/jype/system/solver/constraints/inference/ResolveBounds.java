package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.collect.multi.Pair;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
import honeyroasted.jype.system.solver.constraints.TypeContext;
import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.system.visitor.visitors.MetaVarTypeResolver;
import honeyroasted.jype.system.visitor.visitors.RecursiveTypeVisitor;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ResolveBounds extends ConstraintMapper.All {
    @Override
    protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch) {
        return branch.status().isTrue();
    }

    @Override
    protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch) {
        PropertySet instanceContext = branch.parent().metadata();
        Function<Type, Type> mapper = instanceContext.firstOr(TypeContext.TypeMapper.class, TypeContext.TypeMapper.NO_OP).mapper().apply(branch);

        TypeSystem system = instanceContext.firstOr(TypeSystem.class, TypeSystem.RUNTIME_REFLECTION);

        Map<MetaVarType, Set<MetaVarType>> dependencies = this.discoverDependencies(branch, mapper);

        Set<MetaVarType> varsAndDeps = new LinkedHashSet<>();
        dependencies.forEach((mvt, deps) -> {
            varsAndDeps.add(mvt);
            varsAndDeps.addAll(deps);
        });

        Map<MetaVarType, Type> instantiations = findAllInstantiations(varsAndDeps, branch);
        boolean foundAllInstantiations = instantiations.size() == varsAndDeps.size();

        if (foundAllInstantiations) {
            instantiations.forEach((mvt, t) -> branch.add(new TypeConstraints.Instantiation(mvt, t), Constraint.Status.TRUE));
            return;
        }

        Set<MetaVarType> subset = findSubset(varsAndDeps, dependencies, instantiations);
        if (!subset.isEmpty()) {
            Map<Constraint, Constraint.Status> generatedBounds = new HashMap<>();

            //Bound set does not contain any bound of the form G<..., a_i, ...> = capture(G<...>)
            boolean hasCapture = branch.constraints().entrySet().stream().anyMatch(node -> node.getValue().isTrue() && node.getKey() instanceof TypeConstraints.Capture cpt &&
                    ((ClassType) mapper.apply(cpt.left())).typeArguments().stream().anyMatch(subset::contains));

            if (!hasCapture) {
                Map<MetaVarType, Pair<Type, Constraint>> candidates = new LinkedHashMap<>();
                boolean foundAllCandidates = true;
                for (MetaVarType mvt : subset) {
                    Pair<Map<Type, Constraint>, Map<Type, Constraint>> properBounds = this.findProperBounds(mvt, branch.constraints(), mapper);
                    Map<Type, Constraint> properUpper = properBounds.left();
                    Map<Type, Constraint> properLower = properBounds.right();

                    if (!properLower.isEmpty()) {
                        Type lub = mvt.typeSystem().operations().findLeastUpperBound(properLower.keySet());
                        candidates.put(mvt, Pair.of(lub, new TypeConstraints.Equal(mvt, lub)));
                    } else if (branch.constraints().entrySet().stream().anyMatch(cn -> cn.getValue().isTrue() && cn.getKey() instanceof TypeConstraints.Throws thr && mapper.apply(thr.value()).equals(mvt)) &&
                            properUpper.keySet().stream().allMatch(t -> system.operations().isSubtype(t, mvt.typeSystem().constants().runtimeException()))) {
                        Type cand = mvt.typeSystem().constants().runtimeException();
                        candidates.put(mvt, Pair.of(cand, new TypeConstraints.Equal(mvt, cand)));
                    } else if (!properUpper.isEmpty()) {
                        Type glb = mvt.typeSystem().operations().findGreatestLowerBound(properUpper.keySet());
                        candidates.put(mvt, Pair.of(glb, new TypeConstraints.Equal(mvt, glb)));
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
                Map<MetaVarType, MetaVarType> freshVars = new LinkedHashMap<>();
                varsAndDeps.forEach(mv -> freshVars.put(mv, mv.typeSystem().typeFactory().newMetaVarType(mv.name() + "_y")));
                MetaVarTypeResolver theta = new MetaVarTypeResolver(freshVars);

                Map<Constraint, Constraint.Status> newBounds = new HashMap<>();
                branch.constraints().forEach((con, stat) -> {
                    if (stat.isTrue()) {
                        newBounds.put(con, stat);
                    }
                });

                for (MetaVarType mvt : varsAndDeps) {
                    MetaVarType y = freshVars.get(mvt);

                    Pair<Map<Type, Constraint>, Map<Type, Constraint>> properBounds = this.findProperBounds(mvt, branch.constraints(), mapper);
                    Map<Type, Constraint> properUpper = properBounds.left();
                    Map<Type, Constraint> properLower = properBounds.right();

                    if (!properLower.isEmpty()) {
                        Type lower = mvt.typeSystem().operations().findLeastUpperBound(properLower.keySet());
                        newBounds.put(new TypeConstraints.Equal(mvt, lower), Constraint.Status.ASSUMED);
                        y.lowerBounds().add(lower);
                    }

                    if (!properUpper.isEmpty()) {
                        Type upper = mvt.typeSystem().operations().findGreatestLowerBound(properUpper.keySet().stream().map(theta).collect(Collectors.toCollection(LinkedHashSet::new)));
                        newBounds.put(new TypeConstraints.Equal(mvt, upper), Constraint.Status.ASSUMED);
                        y.upperBounds().add(upper);
                    }
                }

                newBounds.entrySet().removeIf(entry -> entry.getKey() instanceof TypeConstraints.Capture cpt && cpt.left().typeArguments().stream().anyMatch(subset::contains));

                freshVars.forEach((mv, fresh) -> newBounds.put(new TypeConstraints.Equal(mv, fresh), Constraint.Status.ASSUMED));

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

    private Pair<Map<Type, Constraint>, Map<Type, Constraint>> findProperBounds(MetaVarType mvt, Map<Constraint, Constraint.Status> bounds, Function<Type, Type> mapper) {
        Map<Type, Constraint> upperBounds = new LinkedHashMap<>();
        Map<Type, Constraint> lowerBounds = new LinkedHashMap<>();

        bounds.forEach((bound, status) -> {
            if (status.isTrue()) {
                if (bound instanceof TypeConstraints.Subtype st) {
                    Type left = mapper.apply(st.left());
                    Type right = mapper.apply(st.right());

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

    private Set<MetaVarType> findSubset(Set<MetaVarType> currentMetaVars, Map<MetaVarType, Set<MetaVarType>> dependencies, Map<MetaVarType, Type> instantiations) {
        for (MetaVarType mvt : currentMetaVars) {
            Set<MetaVarType> subset = trySubsetWithBase(mvt, dependencies, instantiations, Collections.emptySet());
            if (!subset.isEmpty()) {
                return subset;
            }
        }

        return Collections.emptySet();
    }

    private Set<MetaVarType> trySubsetWithBase(MetaVarType base, Map<MetaVarType, Set<MetaVarType>> dependencies, Map<MetaVarType, Type> instantiations, Set<MetaVarType> building) {
        if (building.contains(base)) return building;

        Set<MetaVarType> res = new HashSet<>(building);
        res.add(base);

        for (MetaVarType dep : dependencies.get(base)) {
            if (base != dep) {
                if (instantiations.containsKey(dep)) {
                    res.add(dep);
                } else {
                    boolean foundEquiv = true;
                    for (Type eq : dep.equalities()) {
                        if (eq instanceof MetaVarType equiv) {
                            Set<MetaVarType> discover = trySubsetWithBase(equiv, dependencies, instantiations, res);
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

    private Map<MetaVarType, Type> findAllInstantiations(Set<MetaVarType> mvts, ConstraintBranch bounds) {
        Map<MetaVarType, Type> instantiations = new LinkedHashMap<>();
        for (MetaVarType mvt : mvts) {
            Type instantiation = instantiations.getOrDefault(mvt, findInstantiation(mvt, bounds));
            if (instantiation == null) {
                return new LinkedHashMap<>();
            } else {
                instantiations.put(mvt, instantiation);
            }
        }

        return instantiations;
    }

    public static Type findInstantiation(MetaVarType mvt, ConstraintBranch bounds) {
        Set<Type> types = new LinkedHashSet<>();
        bounds.constraints().forEach((bound, status) -> {
            if (status.isTrue()) {
                if (bound instanceof TypeConstraints.Equal eq) {
                    if (eq.left().typeEquals(mvt) && eq.right().isProperType()) {
                        types.add(eq.right());
                    } else if (eq.right().typeEquals(mvt) && eq.left().isProperType()) {
                        types.add(eq.left());
                    }
                } else if (bound instanceof TypeConstraints.Instantiation inst) {
                    if (inst.left().typeEquals(mvt)) {
                        types.add(inst.right());
                    }
                }
            }
        });
        return types.isEmpty() ? null : mvt.typeSystem().operations().findMostSpecificType(types);
    }

    public Map<MetaVarType, Set<MetaVarType>> discoverDependencies(ConstraintBranch bounds, Function<Type, Type> mapper) {
        Map<MetaVarType, Set<MetaVarType>> dependencies = new LinkedHashMap<>();
        bounds.constraints().forEach((bound, status) -> {
            if (bound instanceof Constraint.Trinary<?, ?, ?> tri) {
                if (tri.left() instanceof Type lt) {
                    initDependencies(mapper.apply(lt), dependencies);
                }

                if (tri.middle() instanceof Type mt) {
                    initDependencies(mapper.apply(mt), dependencies);
                }

                if (tri.right() instanceof Type rt) {
                    initDependencies(mapper.apply(rt), dependencies);
                }
            } else if (bound instanceof Constraint.Binary<?, ?> bin) {
                if (bin.left() instanceof Type lt) {
                    initDependencies(mapper.apply(lt), dependencies);
                }

                if (bin.right() instanceof Type rt) {
                    initDependencies(mapper.apply(rt), dependencies);
                }
            } else if (bound instanceof Constraint.Unary<?> un && un.value() instanceof Type t) {
                initDependencies(mapper.apply(t), dependencies);
            }
        });

        bounds.constraints().forEach((bound, status) -> {
            if (status.isTrue()) {
                if (bound instanceof TypeConstraints.Equal || bound instanceof TypeConstraints.Subtype) {
                    Constraint.Binary<Type, Type> bin = (Constraint.Binary<Type, Type>) bound;
                    if (mapper.apply(bin.left()) instanceof MetaVarType || mapper.apply(bin.right()) instanceof MetaVarType) {
                        MetaVarType mvt = (MetaVarType) mapper.apply(bin.left() instanceof MetaVarType ? bin.left() : bin.right());
                        Type otherType = mapper.apply(bin.left() instanceof MetaVarType ? bin.right() : bin.left());

                        boolean foundInCapture = false;
                        for (Map.Entry<Constraint, Constraint.Status> entry : bounds.constraints().entrySet()) {
                            Constraint other = entry.getKey();
                            Constraint.Status otherStatus = entry.getValue();
                            if (otherStatus.isTrue() && other != bound) {
                                if (other instanceof TypeConstraints.Capture capture) {
                                    if (discoverMetaVars(mapper.apply(capture.left())).contains(mvt)) {
                                        foundInCapture = true;
                                        break;
                                    }
                                }
                            }
                        }

                        MetaVarType alpha = mvt;
                        if (foundInCapture) {
                            discoverMetaVars(otherType).forEach(beta -> dependencies.get(beta).add(alpha));
                        } else {
                            discoverMetaVars(otherType).forEach(beta -> dependencies.get(alpha).add(beta));
                        }
                    }
                } else if (bound instanceof TypeConstraints.Capture capture) {
                    Set<MetaVarType> mvts = discoverMetaVars(mapper.apply(capture.left()));
                    mvts.addAll(discoverMetaVars(mapper.apply(capture.right())));
                    mvts.forEach(mvt -> dependencies.get(mvt).addAll(mvts));
                }
            }
        });

        Map<MetaVarType, Set<MetaVarType>> previous = dependencies;
        Map<MetaVarType, Set<MetaVarType>> current;
        do {
            current = new LinkedHashMap<>();

            Map<MetaVarType, Set<MetaVarType>> finalCurrent = current;
            Map<MetaVarType, Set<MetaVarType>> finalPrevious = previous;
            previous.forEach((mvt, deps) -> {
                Set<MetaVarType> newDeps = new LinkedHashSet<>(deps);
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

    private static void initDependencies(Type visit, Map<MetaVarType, Set<MetaVarType>> dependencies) {
        discoverMetaVars(visit).forEach(mv -> dependencies.computeIfAbsent(mv, k -> {
            Set<MetaVarType> deps = new LinkedHashSet<>();
            deps.add(k);
            return deps;
        }));
    }

    private static Set<MetaVarType> discoverMetaVars(Type visit) {
        return new HashSet<>(new RecursiveTypeVisitor<MetaVarType, Void>((TypeVisitor.Default) (type, context) -> type instanceof MetaVarType mvt ? mvt : null,
                null, false).visit(visit, new HashMap<>()));
    }
}
