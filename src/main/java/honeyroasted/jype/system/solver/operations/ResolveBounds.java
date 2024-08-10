package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.system.visitor.visitors.MetaVarTypeResolver;
import honeyroasted.jype.system.visitor.visitors.RecursiveTypeVisitor;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.impl.MetaVarTypeImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ResolveBounds implements TypeOperation<Set<TypeBound.Result.Builder>, Pair<Map<MetaVarType, Type>, Set<TypeBound.Result.Builder>>> {
    @Override
    public Pair<Map<MetaVarType, Type>, Set<TypeBound.Result.Builder>> apply(TypeSystem system, Set<TypeBound.Result.Builder> builders) {
        Map<MetaVarType, Set<MetaVarType>> dependencies = this.discoverDependencies(builders);
        return this.resolve(system, dependencies.keySet(), dependencies, new LinkedHashMap<>(), builders);
    }

    private Pair<Map<MetaVarType, Type>, Set<TypeBound.Result.Builder>> resolve(TypeSystem system, Set<MetaVarType> currentMetaVars, Map<MetaVarType, Set<MetaVarType>> dependencies, Map<MetaVarType, Type> instantiations, Set<TypeBound.Result.Builder> bounds) {
        Set<MetaVarType> varsAndDeps = new HashSet<>();
        currentMetaVars.forEach(cmv -> varsAndDeps.addAll(dependencies.getOrDefault(cmv, Collections.emptySet())));

        instantiations = new LinkedHashMap<>(instantiations);

        boolean foundAllInstantiations = true;
        for (MetaVarType mvt : varsAndDeps) {
            Type instantiation = instantiations.getOrDefault(mvt, findInstantiation(mvt, bounds));
            if (instantiation == null) {
                foundAllInstantiations = false;
            } else {
                instantiations.put(mvt, instantiation);
            }
        }

        if (foundAllInstantiations) {
            return Pair.of(instantiations, Collections.emptySet());
        }

        Set<MetaVarType> subset = findSubset(varsAndDeps, dependencies, instantiations);
        if (!subset.isEmpty()) {
            Set<TypeBound.Result.Builder> generatedBounds = new LinkedHashSet<>();

            //Bound set does not contain any bound of the form G<..., a_i, ...> = capture(G<...>)
            boolean hasCapture = bounds.stream().anyMatch(builder -> builder.bound() instanceof TypeBound.Capture cpt &&
                    cpt.left().typeArguments().stream().anyMatch(subset::contains));

            if (!hasCapture) {
                Map<MetaVarType, Pair<Type, TypeBound.Result.Builder>> candidates = new LinkedHashMap<>();
                boolean foundAllCandidates = true;
                for (MetaVarType mvt : subset) {
                    Pair<Map<Type, TypeBound.Result.Builder>, Map<Type, TypeBound.Result.Builder>> properBounds = this.findProperBounds(mvt, bounds);
                    Map<Type, TypeBound.Result.Builder> properUpper = properBounds.left();
                    Map<Type, TypeBound.Result.Builder> properLower = properBounds.right();

                    if (!properLower.isEmpty()) {
                        Type lub = mvt.typeSystem().operations().findLeastUpperBound(properLower.keySet());
                        TypeBound.Result.Builder bound = TypeBound.Result.builder(new TypeBound.Equal(mvt, lub), TypeBound.Result.Propagation.AND, properLower.values()).setSatisfied(true);
                        candidates.put(mvt, Pair.of(lub, bound));
                    } else if (bounds.stream().anyMatch(b -> b.bound() instanceof TypeBound.Throws thr && thr.value().equals(mvt)) &&
                            properUpper.keySet().stream().allMatch(t -> system.operations().isSubtype(t, mvt.typeSystem().constants().runtimeException()))) {
                        Type cand = mvt.typeSystem().constants().runtimeException();
                        TypeBound.Result.Builder bound = TypeBound.Result.builder(new TypeBound.Equal(mvt, cand), TypeBound.Result.Propagation.AND, properLower.values()).setSatisfied(true);
                        candidates.put(mvt, Pair.of(cand, bound));
                    } else if (!properUpper.isEmpty()) {
                        Type glb = mvt.typeSystem().operations().findGreatestLowerBound(properUpper.keySet());
                        TypeBound.Result.Builder bound = TypeBound.Result.builder(new TypeBound.Equal(mvt, glb), TypeBound.Result.Propagation.AND, properLower.values()).setSatisfied(true);
                        candidates.put(mvt, Pair.of(glb, bound));
                    } else {
                        foundAllCandidates = false;
                    }
                }

                if (foundAllCandidates) {
                    List<TypeBound.Result.Builder> newBounds = new ArrayList<>(bounds);
                    Map<MetaVarType, Type> finalInstantiations = instantiations;
                    candidates.forEach((k, v) -> {
                        newBounds.add(v.right());
                        finalInstantiations.put(k, v.left());
                    });

                    Pair<List<TypeBound.Result.Builder>, List<TypeBound.Result.Builder>> incorporation = system.operations()
                            .incorporationApplier().process(system, newBounds, new ArrayList<>());
                    List<TypeBound.Result.Builder> incorporated = incorporation.left();

                    if (!incorporation.right().isEmpty()) {
                        Pair<List<TypeBound.Result.Builder>, List<TypeBound.Result.Builder>> redux = system.operations().reductionApplier().process(system, incorporated, incorporation.right());
                        incorporated = redux.left();
                    }
                    generatedBounds.addAll(incorporated);
                }
            }

            if (hasCapture || generatedBounds.isEmpty() || generatedBounds.stream().anyMatch(b -> b.bound() instanceof TypeBound.False)) {
                Map<MetaVarType, MetaVarType> freshVars = new LinkedHashMap<>();
                varsAndDeps.forEach(mv -> freshVars.put(mv, new MetaVarTypeImpl(mv.typeSystem(), mv.name() + "_y")));
                MetaVarTypeResolver theta = new MetaVarTypeResolver(freshVars);

                Set<TypeBound.Result.Builder> newBounds = new LinkedHashSet<>(bounds);

                for (MetaVarType mvt : varsAndDeps) {
                    MetaVarType y = freshVars.get(mvt);

                    Pair<Map<Type, TypeBound.Result.Builder>, Map<Type, TypeBound.Result.Builder>> properBounds = this.findProperBounds(mvt, bounds);
                    Map<Type, TypeBound.Result.Builder> properUpper = properBounds.left();
                    Map<Type, TypeBound.Result.Builder> properLower = properBounds.right();

                    if (!properLower.isEmpty()) {
                        Type lower = mvt.typeSystem().operations().findLeastUpperBound(properLower.keySet());
                        TypeBound.Result.Builder bound = TypeBound.Result.builder(new TypeBound.Equal(mvt, lower), properLower.values()).setSatisfied(true);
                        newBounds.add(bound);
                        y.lowerBounds().add(lower);
                    }

                    if (!properUpper.isEmpty()) {
                        Type upper = mvt.typeSystem().operations().findGreatestLowerBound(properUpper.keySet().stream().map(theta).collect(Collectors.toCollection(LinkedHashSet::new)));
                        TypeBound.Result.Builder bound = TypeBound.Result.builder(new TypeBound.Equal(mvt, upper), properLower.values()).setSatisfied(true);
                        newBounds.add(bound);
                        y.upperBounds().add(upper);
                    }
                }

                newBounds.removeIf(b -> b.bound() instanceof TypeBound.Capture cpt && cpt.left().typeArguments().stream().anyMatch(subset::contains));
                freshVars.forEach((mv, fresh) -> newBounds.add(TypeBound.Result.builder(new TypeBound.Equal(mv, fresh)).setSatisfied(true)));
                generatedBounds.clear();
                generatedBounds.addAll(newBounds);
            }


            Pair<List<TypeBound.Result.Builder>, List<TypeBound.Result.Builder>> incorporation = system.operations()
                    .incorporationApplier().process(system, new ArrayList<>(generatedBounds), new ArrayList<>());
            List<TypeBound.Result.Builder> incorporated = incorporation.left();

            if (!incorporation.right().isEmpty()) {
                Pair<List<TypeBound.Result.Builder>, List<TypeBound.Result.Builder>> redux = system.operations().reductionApplier().process(system, incorporated, incorporation.right());
                incorporated = redux.left();
            }

            if (incorporated.stream().anyMatch(b -> b.getSatisfied() == TypeBound.Result.Trinary.FALSE)) {
                return Pair.of(Collections.emptyMap(), new LinkedHashSet<>(incorporated));
            } else {
                return this.resolve(system, currentMetaVars, dependencies, instantiations, new LinkedHashSet<>(incorporated));
            }
        } else {
            Set<TypeBound.Result.Builder> boundsFailed = new LinkedHashSet<>(bounds);
            boundsFailed.add(TypeBound.Result.builder(TypeBound.False.INSTANCE, bounds).setSatisfied(false));
            return Pair.of(Collections.emptyMap(), boundsFailed);
        }
    }

    private Pair<Map<Type, TypeBound.Result.Builder>, Map<Type, TypeBound.Result.Builder>> findProperBounds(MetaVarType mvt, Set<TypeBound.Result.Builder> bounds) {
        Map<Type, TypeBound.Result.Builder> upperBounds = new LinkedHashMap<>();
        Map<Type, TypeBound.Result.Builder> lowerBounds = new LinkedHashMap<>();

        for (TypeBound.Result.Builder boundBuilder : bounds) {
            TypeBound bound = boundBuilder.bound();
            if (bound instanceof TypeBound.Subtype st) {
                if (st.left().equals(mvt) && st.right().isProperType()) {
                    upperBounds.put(st.right(), boundBuilder);
                }

                if (st.right().equals(mvt) && st.left().isProperType()) {
                    lowerBounds.put(st.left(), boundBuilder);
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

    private Type findInstantiation(MetaVarType mvt, Set<TypeBound.Result.Builder> bounds) {
        for (TypeBound.Result.Builder boundBuilder : bounds) {
            TypeBound bound = boundBuilder.bound();
            if (bound instanceof TypeBound.Equal eq) {
                if (eq.left().typeEquals(mvt) && eq.right().isProperType()) {
                    TypeBound.Result.builder(new TypeBound.Instantiation(mvt, eq.right()), boundBuilder).setSatisfied(true);
                    return eq.right();
                } else if (eq.right().equals(mvt) && eq.left().isProperType()) {
                    TypeBound.Result.builder(new TypeBound.Instantiation(mvt, eq.left()), boundBuilder).setSatisfied(true);
                    return eq.left();
                }
            }
        }
        return null;
    }

    public Map<MetaVarType, Set<MetaVarType>> discoverDependencies(Set<TypeBound.Result.Builder> bounds) {
        Map<MetaVarType, Set<MetaVarType>> dependencies = new LinkedHashMap<>();

        for (TypeBound.Result.Builder boundBuilder : bounds) {
            TypeBound bound = boundBuilder.bound();
            if (bound instanceof TypeBound.Binary<?, ?> bin) {
                if (bin.left() instanceof Type lt) {
                    initDependencies(lt, dependencies);
                }

                if (bin.right() instanceof Type rt) {
                    initDependencies(rt, dependencies);
                }
            } else if (bound instanceof TypeBound.Unary<?> un && un.value() instanceof Type t) {
                initDependencies(t, dependencies);
            }
        }

        for (TypeBound.Result.Builder boundBuilder : bounds) {
            TypeBound bound = boundBuilder.bound();
            if (bound instanceof TypeBound.Equal || bound instanceof TypeBound.Subtype) {
                TypeBound.Binary<Type, Type> bin = (TypeBound.Binary<Type, Type>) bound;
                MetaVarType mvt = bin.getMetaVar().orElse(null);
                Type otherType = bin.getOtherType().orElse(null);

                boolean foundInCapture = false;
                for (TypeBound.Result.Builder otherBuilder : bounds) {
                    if (boundBuilder == otherBuilder) continue;

                    TypeBound other = otherBuilder.bound();
                    if (other instanceof TypeBound.Capture capture) {
                        if (discoverMetaVars(capture.left()).contains(mvt)) {
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
            } else if (bound instanceof TypeBound.Capture capture) {
                Set<MetaVarType> mvts = discoverMetaVars(capture.left());
                mvts.addAll(discoverMetaVars(capture.right()));
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
