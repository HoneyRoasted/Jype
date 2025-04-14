package honeyroasted.jype.system.solver.operations;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.visitor.JTypeVisitors;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

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

public class JFindLeastUpperBound implements JTypeOperation<Set<JType>, JType> {

    @Override
    public JType apply(JTypeSystem system, Set<JType> types) {
        return findLub(system, types, new HashMap<>());
    }

    private JType findLub(JTypeSystem system, Set<JType> types, Map<Pair<Set<JType>, Set<JType>>, JType> lubCache) {
        if (types.isEmpty()) {
            return system.constants().nullType();
        } else if (types.size() == 1) {
            return types.iterator().next();
        } else {
            Set<Set<JType>> supertypeSets = new LinkedHashSet<>();
            Set<Set<JType>> erasedSupertypeSets = new LinkedHashSet<>();

            for (JType ui : types) {
                Set<JType> st = allSupertypes(ui);
                Set<JType> est = st.stream().map(JTypeVisitors.ERASURE).collect(Collectors.toCollection(LinkedHashSet::new));
                supertypeSets.add(st);
                erasedSupertypeSets.add(est);
            }

            Set<JType> erasedCandidates = intersection(erasedSupertypeSets);
            Set<JType> minimalErasedCandidates = new LinkedHashSet<>();

            for (JType erasedCandidate : erasedCandidates) {
                boolean permitted = true;

                for (JType otherCand : erasedCandidates) {
                    if (!erasedCandidate.typeEquals(otherCand) && system.operations().isSubtype(otherCand, erasedCandidate)) {
                        permitted = false;
                        break;
                    }
                }

                if (permitted) {
                    minimalErasedCandidates.add(erasedCandidate);
                }
            }

            Map<JClassType, Set<JParameterizedClassType>> relevantParams = new LinkedHashMap<>();
            Set<JType> paramKeyPart = new HashSet<>();
            for (JType candidate : minimalErasedCandidates) {
                if (candidate instanceof JClassType ct && ct.hasTypeParameters()) {
                    for (Set<JType> supertypes : supertypeSets) {
                        for (JType supertype : supertypes) {
                            if (supertype instanceof JParameterizedClassType pct && ct.classReference().typeEquals(pct.classReference())) {
                                relevantParams.computeIfAbsent(ct, k -> new LinkedHashSet<>())
                                        .add(pct);
                                paramKeyPart.add(pct);
                            }
                        }
                    }
                }
            }

            Pair<Set<JType>, Set<JType>> key = Pair.of(minimalErasedCandidates, paramKeyPart);

            if (lubCache.containsKey(key)) return lubCache.get(key);

            JIntersectionType type = system.typeFactory().newIntersectionType();
            lubCache.put(key, type);

            Set<JType> lub = new LinkedHashSet<>();
            for (JType candidate : minimalErasedCandidates) {
                if (relevantParams.containsKey(candidate)) {
                    lub.add(leastContainingParameterization(system, lubCache, relevantParams.get(candidate)));
                } else {
                    lub.add(candidate);
                }
            }

            if (lub.size() > 1) {
                lub.remove(system.constants().object());
            }

            type.setChildren(JIntersectionType.flatten(lub));
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

    private JParameterizedClassType leastContainingParameterization(JTypeSystem system, Map<Pair<Set<JType>, Set<JType>>, JType> lubCache, Set<JParameterizedClassType> params) {
        if (params.size() == 1) {
            return leastContainingParameterization(system, lubCache, params.iterator().next());
        }

        Iterator<JParameterizedClassType> iter = params.iterator();

        JParameterizedClassType curr = iter.next();
        while (iter.hasNext()) {
            JParameterizedClassType next = iter.next();
            curr = leastContainingParameterization(system, lubCache, curr, next);
        }
        return curr;
    }

    private JParameterizedClassType leastContainingParameterization(JTypeSystem system, Map<Pair<Set<JType>, Set<JType>>, JType> lubCache, JParameterizedClassType pct) {
        JParameterizedClassType lcta = system.typeFactory().newParameterizedClassType();
        lcta.setClassReference(pct.classReference());

        if (pct.outerType() instanceof JParameterizedClassType outer) {
            lcta.setOuterType(leastContainingParameterization(system, lubCache, outer));
        }

        List<JArgumentType> generated = new ArrayList<>();
        for (int i = 0; i < pct.typeArguments().size(); i++) {
            generated.add(singleLeastContainingTypeArgument(system, lubCache, pct.typeArguments().get(i), pct.typeParameters().get(i)));
        }

        lcta.setTypeArguments(generated);
        lcta.setUnmodifiable(true);

        return lcta;
    }

    private JParameterizedClassType leastContainingParameterization(JTypeSystem system, Map<Pair<Set<JType>, Set<JType>>, JType> lubCache, JParameterizedClassType left, JParameterizedClassType right) {
        JParameterizedClassType lcta = system.typeFactory().newParameterizedClassType();
        lcta.setClassReference(left.classReference());

        if (left.outerType() instanceof JParameterizedClassType lo && right.outerType() instanceof JParameterizedClassType ro) {
            lcta.setOuterType(leastContainingParameterization(system, lubCache, lo, ro));
        }

        List<JArgumentType> generatedArguments = new ArrayList<>();
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

    private JArgumentType singleLeastContainingTypeArgument(JTypeSystem system, Map<Pair<Set<JType>, Set<JType>>, JType> lubCache, JType u, JVarType corresponding) {
        if (u instanceof JWildType.Upper wtu && wtu.hasDefaultBounds()) {
            return wildType(system);
        } else if (u instanceof JVarType vt && vt.hasDefaultBounds()) {
            return wildType(system);
        } else if (corresponding.hasDefaultBounds()) {
            return wildType(system);
        }

        return this.lubWild(system, lubCache, u, u.typeSystem().constants().object());
    }

    private JArgumentType leastContainingTypeArgument(JTypeSystem system, Map<Pair<Set<JType>, Set<JType>>, JType> lubCache, JArgumentType u, JArgumentType v) {
        if (u.typeEquals(v)) return u;

        if (u instanceof JWildType || v instanceof JWildType) {
            JWildType wild = (JWildType) (u instanceof JWildType ? u : v);
            JType other = u instanceof JWildType ? v : u;

            if (wild instanceof JWildType.Upper wtu) {
                if (other instanceof JWildType.Upper owt) {
                    return lubWild(lubCache, system, wtu.upperBounds(), owt.upperBounds());
                } else if (other instanceof JWildType.Lower owl) {
                    return wildType(system);
                } else {
                    return lubWild(system, lubCache, wtu.upperBounds(), v);
                }
            } else if (wild instanceof JWildType.Lower wtl) {
                if (other instanceof JWildType.Upper owt) {
                    return wildType(system);
                } else if (other instanceof JWildType.Lower owl) {
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

    private JWildType.Lower glbWild(JTypeSystem system, Set<JType> left, Set<JType> right) {
        Set<JType> bounds = new LinkedHashSet<>();
        bounds.addAll(left);
        bounds.addAll(right);

        JWildType.Lower lower = system.typeFactory().newLowerWildType();
        lower.setLowerBounds(bounds);
        lower.setUnmodifiable(true);
        return lower;
    }

    private JWildType.Upper wildType(JTypeSystem system) {
        JWildType.Upper wild = system.typeFactory().newUpperWildType();
        wild.setUpperBounds(Set.of(system.constants().object()));
        wild.setUnmodifiable(true);
        return wild;
    }

    private JWildType.Upper lubWild(JTypeSystem system, Map<Pair<Set<JType>, Set<JType>>, JType> lubCache, JType... types) {
        JWildType.Upper result = system.typeFactory().newUpperWildType();
        result.setUpperBounds(Set.of(this.findLub(system, Set.of(types), lubCache)));
        result.setUnmodifiable(true);
        return result;
    }

    private JWildType.Upper lubWild(JTypeSystem system, Map<Pair<Set<JType>, Set<JType>>, JType> lubCache, Set<JType> set, JType... types) {
        Set<JType> bounds = new LinkedHashSet<>(set);
        Collections.addAll(bounds, types);

        JWildType.Upper result = system.typeFactory().newUpperWildType();
        result.setUpperBounds(Set.of(this.findLub(system, bounds, lubCache)));
        result.setUnmodifiable(true);
        return result;
    }

    private JWildType.Upper lubWild(Map<Pair<Set<JType>, Set<JType>>, JType> lubCache, JTypeSystem system, Set<JType> set, Set<JType> other) {
        Set<JType> bounds = new LinkedHashSet<>(set);
        bounds.addAll(other);

        JWildType.Upper result = system.typeFactory().newUpperWildType();
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

    private Set<JType> allSupertypes(JType type) {
        Set<JType> result = new LinkedHashSet<>();
        allSupertypes(type, result);
        return result;
    }

    private void allSupertypes(JType type, Set<JType> types) {
        if (!types.contains(type)) {
            types.add(type);
            type.knownDirectSupertypes().forEach(t -> allSupertypes(t, types));
        }
    }
}
