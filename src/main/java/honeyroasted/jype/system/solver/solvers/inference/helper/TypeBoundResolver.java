package honeyroasted.jype.system.solver.solvers.inference.helper;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.solver.TypeBound;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.system.visitor.visitors.RecursiveTypeVisitor;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.impl.IntersectionTypeImpl;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeBoundResolver extends AbstractInferenceHelper {
    private TypeBoundIncorporater boundIncorporater;

    public TypeBoundResolver() {
        this(TypeSolver.NO_OP);
    }

    public TypeBoundResolver(TypeSolver solver) {
        super(solver);
        this.boundIncorporater = new TypeBoundIncorporater(solver);
    }

    private Map<MetaVarType, Type> instantiations = new LinkedHashMap<>();
    private Set<TypeBound.Result.Builder> initialBounds = new LinkedHashSet<>();
    private Set<TypeBound.Result.Builder> bounds = new LinkedHashSet<>();
    private Set<TypeBound.Result.Builder> constraints = new LinkedHashSet<>();

    public void reset() {
        this.bounds.clear();
        this.initialBounds.clear();
        this.instantiations.clear();
        this.boundIncorporater.reset();
    }

    public TypeBoundResolver addInitialBounds(TypeBound.Result.Builder... bounds) {
        Collections.addAll(initialBounds, bounds);
        return this;
    }

    public TypeBoundResolver addInitialBounds(Collection<TypeBound.Result.Builder> bounds) {
        this.initialBounds.addAll(bounds);
        return this;
    }

    public TypeBoundResolver resolve() {
        this.bounds.addAll(this.initialBounds);
        Map<MetaVarType, Set<MetaVarType>> dependencies = this.discoverDependencies(bounds);
        this.resolve(dependencies.keySet(), dependencies);
        return this;
    }

    private boolean resolve(Set<MetaVarType> currentMetaVars, Map<MetaVarType, Set<MetaVarType>> dependencies) {
        Set<MetaVarType> v = new HashSet<>();
        currentMetaVars.forEach(cmv -> v.addAll(dependencies.getOrDefault(cmv, Collections.emptySet())));

        boolean foundAllInstantiations = true;
        for (MetaVarType mvt : v) {
            Type instantiation = this.instantiations.getOrDefault(mvt, findInstantiation(mvt));
            if (instantiation == null) {
                foundAllInstantiations = false;
            } else {
                this.instantiations.put(mvt, instantiation);
                TypeBound.Result.Builder builder = TypeBound.Result.builder(new TypeBound.Instantiation(mvt, instantiation))
                        .addParents(this.initialBounds).setSatisfied(true);
                this.bounds.add(builder);
                this.initialBounds.forEach(b -> b.addChildren(builder));
            }
        }

        if (!foundAllInstantiations) {
            Set<MetaVarType> subset = findSubset(currentMetaVars, dependencies);
            if (!subset.isEmpty()) {

                return true;
            } else {
                return false;
            }
        }

        return true;
    }

    private Set<MetaVarType> findSubset(Set<MetaVarType> currentMetaVars, Map<MetaVarType, Set<MetaVarType>> dependencies) {
        for (MetaVarType mvt : currentMetaVars) {
            if (dependencies.get(mvt).stream().allMatch(beta -> beta == mvt || this.instantiations.containsKey(beta))) {
                return Set.of(mvt);
            }
        }

        return findSubsetRecursive(new HashSet<>(), currentMetaVars, dependencies);
    }

    private Set<MetaVarType> findSubsetRecursive(Set<MetaVarType> subset, Set<MetaVarType> all, Map<MetaVarType, Set<MetaVarType>> dependencies) {
        //TODO improve algorithm
        Queue<Pair<Set<MetaVarType>, Set<MetaVarType>>> toProcess = new ArrayDeque<>(all.size());

        for (MetaVarType mvt : all) {
            if (this.isResolvableMetaVar(mvt, dependencies)) {
                Set<MetaVarType> newSubset = new HashSet<>(subset);
                newSubset.add(mvt);
                if (this.isValidResolutionSubset(newSubset, dependencies)) {
                    return newSubset;
                }

                Set<MetaVarType> newAll = new HashSet<>(all);
                newAll.remove(mvt);
                toProcess.add(Pair.of(newSubset, newAll));
            }
        }

        while (!toProcess.isEmpty()) {
            Pair<Set<MetaVarType>, Set<MetaVarType>> pair = toProcess.poll();
            Set<MetaVarType> res = this.findSubsetRecursive(pair.left(), pair.right(), dependencies);
            if (!res.isEmpty()) return res;
        }

        return Collections.emptySet();
    }

    private boolean isResolvableMetaVar(MetaVarType mvt, Map<MetaVarType, Set<MetaVarType>> dependencies) {
        return dependencies.get(mvt).stream().allMatch(dep -> mvt == dep || this.instantiations.containsKey(dep) || dep.equalities().stream().anyMatch(t -> t instanceof MetaVarType));
    }

    private boolean isValidResolutionSubset(Set<MetaVarType> subset, Map<MetaVarType, Set<MetaVarType>> dependencies) {
        return !subset.isEmpty() && subset.stream().allMatch(mvt -> dependencies.get(mvt).stream().allMatch(dep -> this.instantiations.containsKey(dep) || subset.stream().anyMatch(dmvt -> dmvt.typeEquals(dep))));
    }

    private Type findInstantiation(MetaVarType mvt) {
        Optional<Type> properEquality = mvt.equalities().stream().filter(Type::isProperType).findFirst();

        if (properEquality.isPresent()) {
            return properEquality.get();
        }

        Set<Type> properUpperBounds = mvt.upperBounds().stream().filter(Type::isProperType).collect(Collectors.toSet());

        if (properUpperBounds.isEmpty()) {
            return null;
        } else if (properUpperBounds.size() == 1) {
            return properUpperBounds.iterator().next();
        } else {
            IntersectionType type = new IntersectionTypeImpl(mvt.typeSystem());
            type.setChildren(IntersectionType.flatten(properUpperBounds));
            return type;
        }
    }

    public Map<MetaVarType, Set<MetaVarType>> discoverDependencies(Set<TypeBound.Result.Builder> bounds) {
        Map<MetaVarType, Set<MetaVarType>> dependencies = new LinkedHashMap<>();

        for (TypeBound.Result.Builder boundBuilder : bounds) {
            TypeBound bound = boundBuilder.bound();
            if (bound instanceof TypeBound.Binary<?, ?> bin && bin.left() instanceof Type lt && bin.right() instanceof Type rt) {
                initDependencies(lt, dependencies);
                initDependencies(rt, dependencies);
            } else if (bound instanceof TypeBound.Unary<?> un && un.value() instanceof Type t) {
                initDependencies(t, dependencies);
            }
        }

        for (TypeBound.Result.Builder boundBuilder : bounds) {
            TypeBound bound = boundBuilder.bound();
            if (bound instanceof TypeBound.Equal || bound instanceof TypeBound.Subtype) {
                TypeBound.Binary<Type, Type> bin = (TypeBound.Binary<Type, Type>) bound;
                MetaVarType mvt = getMetaVarType(bin);
                Type otherType = getOtherType(bin);

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
                null, false).visit(visit));
    }

    private static MetaVarType getMetaVarType(TypeBound.Binary<? extends Type, ? extends Type> bound) {
        if (bound.left() instanceof MetaVarType m) {
            return m;
        } else if (bound.right() instanceof MetaVarType m) {
            return m;
        } else {
            return null;
        }
    }

    private static boolean hasMetaVarType(TypeBound.Binary<? extends Type, ? extends Type> bound) {
        return getMetaVarType(bound) != null;
    }

    private static Type getOtherType(TypeBound.Binary<? extends Type, ? extends Type> bound) {
        if (bound.left() instanceof MetaVarType) {
            return bound.right();
        } else {
            return bound.left();
        }
    }

}
