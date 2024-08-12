package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.TypeVisitors;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FindLeastUpperBound implements TypeOperation<Set<Type>, Type> {

    @Override
    public Type apply(TypeSystem system, Set<Type> types) {
        return findLub(system, types, new HashMap<>());

    }

    private Type findLub(TypeSystem system, Set<Type> types, Map<Pair<Set<Type>, Set<Type>>, Type> lubCache) {
        if (types.isEmpty()) {
            return system.constants().nullType();
        } else if (types.size() == 1) {
            return types.iterator().next();
        } else {
            Set<Set<Type>> supertypeSets = new LinkedHashSet<>();
            Set<Set<Type>> erasedSupertypeSets = new LinkedHashSet<>();

            for (Type ui : types) {
                Set<Type> st = allSupertypes(ui);
                Set<Type> est = st.stream().map(TypeVisitors.ERASURE).collect(Collectors.toCollection(LinkedHashSet::new));
                supertypeSets.add(st);
                erasedSupertypeSets.add(est);
            }

            Set<Type> erasedCandidates = intersection(erasedSupertypeSets);
            Set<Type> minimalErasedCandidates = new LinkedHashSet<>();

            for (Type erasedCandidate : erasedCandidates) {
                boolean permitted = true;

                for (Type otherCand : erasedCandidates) {
                    if (!erasedCandidate.typeEquals(otherCand) && system.operations().isSubtype(erasedCandidate, otherCand)) {
                        permitted = false;
                        break;
                    }
                }

                if (permitted) {
                    minimalErasedCandidates.add(erasedCandidate);
                }
            }

            Map<ClassType, Set<ParameterizedClassType>> relevantParams = new LinkedHashMap<>();
            Set<Type> paramKeyPart = new HashSet<>();
            for (Type candidate : minimalErasedCandidates) {
                if (candidate instanceof ClassType ct && ct.hasTypeParameters()) {
                    for (Set<Type> supertypes : supertypeSets) {
                        for (Type supertype : supertypes) {
                            if (supertype instanceof ParameterizedClassType pct && ct.classReference().typeEquals(pct.classReference())) {
                                relevantParams.computeIfAbsent(ct, k -> new LinkedHashSet<>())
                                        .add(pct);
                                paramKeyPart.add(pct);
                            }
                        }
                    }
                }
            }

            Pair<Set<Type>, Set<Type>> key = Pair.of(minimalErasedCandidates, paramKeyPart);

            if (lubCache.containsKey(key)) return lubCache.get(key);

            IntersectionType type = system.typeFactory().newIntersectionType();
            lubCache.put(key, type);

            Set<Type> lub = new LinkedHashSet<>();
            for (Type candidate : minimalErasedCandidates) {
                if (relevantParams.containsKey(candidate)) {
                    lub.add(leastContainingParameterization(system, lubCache, relevantParams.get(candidate)));
                } else {
                    lub.add(candidate);
                }
            }

            if (lub.size() > 1) {
                lub.remove(system.constants().object());
            }

            type.setChildren(IntersectionType.flatten(lub));
            type.setUnmodifiable(true);

            if (lub.isEmpty()) {
                return system.constants().nullType();
            } else if (lub.size() == 1) {
                return lub.iterator().next();
            } else {
                return type.simplify();
            }
        }
    }

    private ParameterizedClassType leastContainingParameterization(TypeSystem system, Map<Pair<Set<Type>, Set<Type>>, Type> lubCache, Set<ParameterizedClassType> params) {
        if (params.size() == 1) {
            return leastContainingParameterization(system, lubCache, params.iterator().next());
        }

        Iterator<ParameterizedClassType> iter = params.iterator();

        ParameterizedClassType curr = iter.next();
        while (iter.hasNext()) {
            ParameterizedClassType next = iter.next();
            curr = leastContainingParameterization(system, lubCache, curr, next);
        }
        return curr;
    }

    private ParameterizedClassType leastContainingParameterization(TypeSystem system, Map<Pair<Set<Type>, Set<Type>>, Type> lubCache, ParameterizedClassType pct) {
        ParameterizedClassType lcta = system.typeFactory().newParameterizedClassType();
        lcta.setClassReference(pct.classReference());

        if (pct.outerType() instanceof ParameterizedClassType outer) {
            lcta.setOuterType(leastContainingParameterization(system, lubCache, outer));
        }

        List<ArgumentType> generated = new ArrayList<>();
        for (int i = 0; i < pct.typeArguments().size(); i++) {
            generated.add(singleLeastContainingTypeArgument(system, lubCache, pct.typeArguments().get(i), pct.typeParameters().get(i)));
        }

        lcta.setTypeArguments(generated);
        lcta.setUnmodifiable(true);

        return lcta;
    }

    private ParameterizedClassType leastContainingParameterization(TypeSystem system, Map<Pair<Set<Type>, Set<Type>>, Type> lubCache, ParameterizedClassType left, ParameterizedClassType right) {
        ParameterizedClassType lcta = system.typeFactory().newParameterizedClassType();
        lcta.setClassReference(left.classReference());

        if (left.outerType() instanceof ParameterizedClassType lo && right.outerType() instanceof ParameterizedClassType ro) {
            lcta.setOuterType(leastContainingParameterization(system, lubCache, lo, ro));
        }

        List<ArgumentType> generatedArguments = new ArrayList<>();
        for (int i = 0; i < Math.max(left.typeArguments().size(), right.typeArguments().size()); i++) {
            if (i < left.typeArguments().size() && i < right.typeArguments().size()) {
                generatedArguments.add(leastContainingTypeArgument(system, lubCache, left.typeArguments().get(i), right.typeArguments().get(i)));
            } else if (i < left.typeArguments().size()) {
                generatedArguments.add(singleLeastContainingTypeArgument(system, lubCache, left.typeArguments().get(i), left.typeParameters().get(i)));
            } else {
                generatedArguments.add(singleLeastContainingTypeArgument(system, lubCache, right.typeArguments().get(i), right.typeParameters().get(i)));
            }
        }
        lcta.setTypeArguments(generatedArguments);
        lcta.setUnmodifiable(true);

        return lcta;
    }

    private ArgumentType singleLeastContainingTypeArgument(TypeSystem system, Map<Pair<Set<Type>, Set<Type>>, Type> lubCache, Type u, VarType corresponding) {
        if (u instanceof WildType.Upper wtu && wtu.hasDefaultBounds()) {
            return wildType(system);
        } else if (u instanceof VarType vt && vt.hasDefaultBounds()) {
            return wildType(system);
        } else if (corresponding.hasDefaultBounds()) {
            return wildType(system);
        }

        return this.lubWild(system, lubCache, u, u.typeSystem().constants().object());
    }

    private ArgumentType leastContainingTypeArgument(TypeSystem system, Map<Pair<Set<Type>, Set<Type>>, Type> lubCache, ArgumentType u, ArgumentType v) {
        if (u.typeEquals(v)) return u;

        if (u instanceof WildType || v instanceof WildType) {
            WildType wild = (WildType) (u instanceof WildType ? u : v);
            Type other = u instanceof WildType ? v : u;

            if (wild instanceof WildType.Upper wtu) {
                if (other instanceof WildType.Upper owt) {
                    return lubWild(lubCache, system, wtu.upperBounds(), owt.upperBounds());
                } else if (other instanceof WildType.Lower owl) {
                    return wildType(system);
                } else {
                    return lubWild(system, lubCache, wtu.upperBounds(), v);
                }
            } else if (wild instanceof WildType.Lower wtl) {
                if (other instanceof WildType.Upper owt) {
                    return wildType(system);
                } else if (other instanceof WildType.Lower owl) {
                    return glbWild(system, wtl.lowerBounds(), owl.lowerBounds());
                } else {
                    return lubWild(system, lubCache, wtl.lowerBound(), other);
                }
            } else {
                return lubWild(system, lubCache, u, v);
            }
        } else {
            return lubWild(system, lubCache, u, v);
        }
    }

    private WildType.Lower glbWild(TypeSystem system, Set<Type> left, Set<Type> right) {
        Set<Type> bounds = new LinkedHashSet<>();
        bounds.addAll(left);
        bounds.addAll(right);

        WildType.Lower lower = system.typeFactory().newLowerWildType();
        lower.setLowerBounds(bounds);
        lower.setUnmodifiable(true);
        return lower;
    }

    private WildType.Upper wildType(TypeSystem system) {
        WildType.Upper wild = system.typeFactory().newUpperWildType();
        wild.setUpperBounds(Set.of(system.constants().object()));
        wild.setUnmodifiable(true);
        return wild;
    }

    private WildType.Upper lubWild(TypeSystem system, Map<Pair<Set<Type>, Set<Type>>, Type> lubCache, Type... types) {
        WildType.Upper result = system.typeFactory().newUpperWildType();
        result.setUpperBounds(Set.of(this.findLub(system, Set.of(types), lubCache)));
        result.setUnmodifiable(true);
        return result;
    }

    private WildType.Upper lubWild(TypeSystem system, Map<Pair<Set<Type>, Set<Type>>, Type> lubCache, Set<Type> set, Type... types) {
        Set<Type> bounds = new LinkedHashSet<>(set);
        Collections.addAll(bounds, types);

        WildType.Upper result = system.typeFactory().newUpperWildType();
        result.setUpperBounds(Set.of(this.findLub(system, bounds, lubCache)));
        result.setUnmodifiable(true);
        return result;
    }

    private WildType.Upper lubWild(Map<Pair<Set<Type>, Set<Type>>, Type> lubCache, TypeSystem system, Set<Type> set, Set<Type> other) {
        Set<Type> bounds = new LinkedHashSet<>(set);
        bounds.addAll(other);

        WildType.Upper result = system.typeFactory().newUpperWildType();
        result.setUpperBounds(Set.of(this.findLub(system, bounds, lubCache)));
        result.setUnmodifiable(true);
        return result;
    }

    private <T> Set<T> intersection(Collection<? extends Set<? extends T>> sets) {
        Set<T> result = new LinkedHashSet<>();
        sets.forEach(result::addAll);
        sets.forEach(result::retainAll);
        return result;
    }

    private Set<Type> allSupertypes(Type type) {
        Set<Type> result = new LinkedHashSet<>();
        allSupertypes(type, result);
        return result;
    }

    private void allSupertypes(Type type, Set<Type> types) {
        if (!types.contains(type)) {
            types.add(type);
            type.knownDirectSupertypes().forEach(t -> allSupertypes(t, types));
        }
    }
}
