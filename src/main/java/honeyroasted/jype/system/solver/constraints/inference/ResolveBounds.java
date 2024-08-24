package honeyroasted.jype.system.solver.constraints.inference;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.almonds.TrackedConstraint;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.multi.Pair;
import honeyroasted.collect.property.PropertySet;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.constraints.TypeConstraints;
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

public class ResolveBounds implements ConstraintMapper {

    @Override
    public int arity() {
        return PARENT_BRANCH_NODE;
    }

    @Override
    public boolean filter(PropertySet context, ConstraintNode node) {
        return node.neighbors(ConstraintNode.Operation.AND).stream()
                .noneMatch(cn -> cn.constraint() instanceof TypeConstraints.Instantiation);
    }

    @Override
    public boolean accepts(PropertySet context, ConstraintNode... nodes) {
        return true;
    }

    @Override
    public void process(PropertySet context, ConstraintNode... nodes) {
        ConstraintTree bounds = nodes[0].expandRoot(ConstraintNode.Operation.AND);
        Function<Type, Type> mapper = context.firstOr(TypeConstraints.TypeMapper.class, TypeConstraints.NO_OP).mapper().apply(bounds);

        TypeSystem system = context.firstOr(TypeSystem.class, TypeSystem.SIMPLE_RUNTIME);

        Map<MetaVarType, Set<MetaVarType>> dependencies = this.discoverDependencies(bounds, mapper);

        Set<MetaVarType> varsAndDeps = new LinkedHashSet<>();
        dependencies.forEach((mvt, deps) -> {
            varsAndDeps.add(mvt);
            varsAndDeps.addAll(deps);
        });

        Map<MetaVarType, Type> instantiations = findAllInstantiations(varsAndDeps, bounds);
        boolean foundAllInstantiations = instantiations.size() == varsAndDeps.size();

        if (foundAllInstantiations) {
            instantiations.forEach((mvt, t) -> bounds.attach(new TypeConstraints.Instantiation(mvt, t).tracked(
                    bounds.neighbors(ConstraintNode.Operation.AND, ConstraintNode.Status.TRUE).stream().map(ConstraintNode::trackedConstraint).toArray(TrackedConstraint[]::new)
            ).createLeaf().overrideStatus(true)));
            return;
        }

        Set<MetaVarType> subset = findSubset(varsAndDeps, dependencies, instantiations);
        if (!subset.isEmpty()) {
            ConstraintTree generatedBounds = new ConstraintTree(Constraint.and().tracked(), ConstraintNode.Operation.AND);

            //Bound set does not contain any bound of the form G<..., a_i, ...> = capture(G<...>)
            boolean hasCapture = bounds.neighbors(ConstraintNode.Operation.AND, ConstraintNode.Status.TRUE).stream().anyMatch(node -> node.constraint() instanceof TypeConstraints.Capture cpt &&
                    ((ClassType) mapper.apply(cpt.left())).typeArguments().stream().anyMatch(subset::contains));

            if (!hasCapture) {
                Map<MetaVarType, Pair<Type, TrackedConstraint>> candidates = new LinkedHashMap<>();
                boolean foundAllCandidates = true;
                for (MetaVarType mvt : subset) {
                    Pair<Map<Type, TrackedConstraint>, Map<Type, TrackedConstraint>> properBounds = this.findProperBounds(mvt, bounds, mapper);
                    Map<Type, TrackedConstraint> properUpper = properBounds.left();
                    Map<Type, TrackedConstraint> properLower = properBounds.right();

                    if (!properLower.isEmpty()) {
                        Type lub = mvt.typeSystem().operations().findLeastUpperBound(properLower.keySet());
                        candidates.put(mvt, Pair.of(lub, new TypeConstraints.Equal(mvt, lub).tracked(properLower.values().toArray(TrackedConstraint[]::new))));
                    } else if (bounds.neighbors(ConstraintNode.Operation.AND, ConstraintNode.Status.TRUE).stream().anyMatch(cn -> cn.constraint() instanceof TypeConstraints.Throws thr && mapper.apply(thr.value()).equals(mvt)) &&
                            properUpper.keySet().stream().allMatch(t -> system.operations().isSubtype(t, mvt.typeSystem().constants().runtimeException()))) {
                        Type cand = mvt.typeSystem().constants().runtimeException();
                        candidates.put(mvt, Pair.of(cand, new TypeConstraints.Equal(mvt, cand).tracked(properLower.values().toArray(TrackedConstraint[]::new))));
                    } else if (!properUpper.isEmpty()) {
                        Type glb = mvt.typeSystem().operations().findGreatestLowerBound(properUpper.keySet());
                        candidates.put(mvt, Pair.of(glb, new TypeConstraints.Equal(mvt, glb).tracked(properLower.values().toArray(TrackedConstraint[]::new))));
                    } else {
                        foundAllCandidates = false;
                    }
                }

                if (foundAllCandidates) {
                    ConstraintTree newBounds = bounds.root(ConstraintNode.Operation.AND).copy().expandInPlace(ConstraintNode.Operation.AND);
                    candidates.forEach((k, v) -> {
                        newBounds.attach(v.right());
                        instantiations.put(k, v.left());
                    });

                    ConstraintNode incorp = system.operations().incorporationApplier().process(newBounds);
                    incorp = system.operations().reductionApplier().process(incorp);
                    incorp.visitNeighbors(ConstraintNode.Operation.AND, ConstraintNode::satisfied, generatedBounds::attach);
                }
            }

            if (hasCapture || generatedBounds.children().isEmpty() || generatedBounds.neighbors(ConstraintNode.Operation.AND).stream().anyMatch(b -> b.constraint() instanceof Constraint.False)) {
                Map<MetaVarType, MetaVarType> freshVars = new LinkedHashMap<>();
                varsAndDeps.forEach(mv -> freshVars.put(mv, mv.typeSystem().typeFactory().newMetaVarType(mv.name() + "_y")));
                MetaVarTypeResolver theta = new MetaVarTypeResolver(freshVars);

                ConstraintTree newBounds = new ConstraintTree(Constraint.and().tracked(), ConstraintNode.Operation.AND);
                bounds.visitNeighbors(ConstraintNode.Operation.AND, ConstraintNode::satisfied, cn -> newBounds.attach(cn.trackedConstraint()));

                for (MetaVarType mvt : varsAndDeps) {
                    MetaVarType y = freshVars.get(mvt);

                    Pair<Map<Type, TrackedConstraint>, Map<Type, TrackedConstraint>> properBounds = this.findProperBounds(mvt, bounds, mapper);
                    Map<Type, TrackedConstraint> properUpper = properBounds.left();
                    Map<Type, TrackedConstraint> properLower = properBounds.right();

                    if (!properLower.isEmpty()) {
                        Type lower = mvt.typeSystem().operations().findLeastUpperBound(properLower.keySet());
                        TrackedConstraint bound = TrackedConstraint.of(new TypeConstraints.Equal(mvt, lower), properLower.values().toArray(TrackedConstraint[]::new));
                        newBounds.attach(bound);
                        y.lowerBounds().add(lower);
                    }

                    if (!properUpper.isEmpty()) {
                        Type upper = mvt.typeSystem().operations().findGreatestLowerBound(properUpper.keySet().stream().map(theta).collect(Collectors.toCollection(LinkedHashSet::new)));
                        TrackedConstraint bound = TrackedConstraint.of(new TypeConstraints.Equal(mvt, upper), properLower.values().toArray(TrackedConstraint[]::new));
                        newBounds.attach(bound);
                        y.upperBounds().add(upper);
                    }
                }

                newBounds.neighbors(ConstraintNode.Operation.AND, ConstraintNode::satisfied)
                        .stream()
                        .filter(cn -> cn.parent() != null && cn.constraint() instanceof TypeConstraints.Capture cpt && cpt.left().typeArguments().stream().anyMatch(subset::contains))
                        .forEach(cn -> cn.parent().detach(cn));


                freshVars.forEach((mv, fresh) -> newBounds.attach(TrackedConstraint.of(new TypeConstraints.Equal(mv, fresh))));

                generatedBounds = newBounds;
            }


            ConstraintNode incorp = generatedBounds.copy();
            incorp = system.operations().incorporationApplier().process(incorp);
            incorp = system.operations().reductionApplier().process(incorp);


            if (incorp.satisfied()) {
                ConstraintNode finalIncorp = incorp;
                findAllInstantiations(varsAndDeps, incorp).forEach((mvt, t) -> bounds.attach(new TypeConstraints.Instantiation(mvt, t).tracked(
                        finalIncorp.neighbors(ConstraintNode.Operation.AND, ConstraintNode.Status.TRUE).stream().map(ConstraintNode::trackedConstraint).toArray(TrackedConstraint[]::new)
                ).createLeaf().overrideStatus(true)));
            }
        } else {
            bounds.attach(Constraint.FALSE.tracked(bounds.neighbors(ConstraintNode.Operation.AND, ConstraintNode.Status.TRUE).stream().map(ConstraintNode::trackedConstraint).toArray(TrackedConstraint[]::new)));
        }

    }


    private Pair<Map<Type, TrackedConstraint>, Map<Type, TrackedConstraint>> findProperBounds(MetaVarType mvt, ConstraintNode bounds, Function<Type, Type> mapper) {
        Map<Type, TrackedConstraint> upperBounds = new LinkedHashMap<>();
        Map<Type, TrackedConstraint> lowerBounds = new LinkedHashMap<>();

        for (ConstraintNode node : bounds.neighbors(ConstraintNode.Operation.AND, ConstraintNode.Status.TRUE)) {
            Constraint bound = node.constraint();
            if (bound instanceof TypeConstraints.Subtype st) {
                Type left = mapper.apply(st.left());
                Type right = mapper.apply(st.right());

                if (left.equals(mvt) && right.isProperType()) {
                    upperBounds.put(right, node.trackedConstraint());
                }

                if (right.equals(mvt) && left.isProperType()) {
                    lowerBounds.put(left, node.trackedConstraint());
                }
            }
        }

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

    private Map<MetaVarType, Type> findAllInstantiations(Set<MetaVarType> mvts, ConstraintNode bounds) {
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

    private Type findInstantiation(MetaVarType mvt, ConstraintNode bounds) {
        Set<Type> types = new LinkedHashSet<>();

        for (ConstraintNode node : bounds.neighbors(ConstraintNode.Operation.AND, ConstraintNode.Status.TRUE)) {
            Constraint constraint = node.constraint();
            if (constraint instanceof TypeConstraints.Equal eq) {
                if (eq.left().typeEquals(mvt) && eq.right().isProperType()) {
                    types.add(eq.right());
                } else if (eq.right().typeEquals(mvt) && eq.left().isProperType()) {
                    types.add(eq.left());
                }
            } else if (constraint instanceof TypeConstraints.Instantiation inst) {
                if (inst.left().typeEquals(mvt)) {
                    types.add(inst.right());
                }
            }
        }

        return types.isEmpty() ? null : mvt.typeSystem().operations().findMostSpecificType(types);
    }

    public Map<MetaVarType, Set<MetaVarType>> discoverDependencies(ConstraintNode bounds, Function<Type, Type> mapper) {
        Map<MetaVarType, Set<MetaVarType>> dependencies = new LinkedHashMap<>();

        for (ConstraintNode node : bounds.neighbors(ConstraintNode.Operation.AND, ConstraintNode.Status.TRUE)) {
            Constraint bound = node.constraint();
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
        }

        for (ConstraintNode node : bounds.neighbors(ConstraintNode.Operation.AND, ConstraintNode.Status.TRUE)) {
            Constraint bound = node.constraint();
            if (bound instanceof TypeConstraints.Equal || bound instanceof TypeConstraints.Subtype) {
                Constraint.Binary<Type, Type> bin = (Constraint.Binary<Type, Type>) bound;
                if (mapper.apply(bin.left()) instanceof MetaVarType || mapper.apply(bin.right()) instanceof MetaVarType) {
                    MetaVarType mvt = (MetaVarType) mapper.apply(bin.left() instanceof MetaVarType ? bin.left() : bin.right());
                    Type otherType = mapper.apply(bin.left() instanceof MetaVarType ? bin.right() : bin.left());

                    boolean foundInCapture = false;
                    for (ConstraintNode otherNode : bounds.neighbors(ConstraintNode.Operation.AND, ConstraintNode.Status.TRUE)) {
                        if (otherNode == node) continue;

                        Constraint other = otherNode.constraint();
                        if (other instanceof TypeConstraints.Capture capture) {
                            if (discoverMetaVars(mapper.apply(capture.left())).contains(mvt)) {
                                foundInCapture = true;
                                break;
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
