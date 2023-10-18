package honeyroasted.jype.system.solver.solvers.inference.helper;

import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.visitor.TypeVisitors;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.ParameterizedClassType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;
import honeyroasted.jype.type.impl.IntersectionTypeImpl;
import honeyroasted.jype.type.impl.ParameterizedClassTypeImpl;
import honeyroasted.jype.type.impl.WildTypeLowerImpl;
import honeyroasted.jype.type.impl.WildTypeUpperImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeLubFinder extends AbstractInferenceHelper {
    private TypeCompatibilityChecker typeCompatibilityChecker;

    public TypeLubFinder() {
        this.typeCompatibilityChecker = new TypeCompatibilityChecker();
    }

    public TypeLubFinder(TypeSolver solver) {
        super(solver);
        this.typeCompatibilityChecker = new TypeCompatibilityChecker(solver);
    }

    public Type find(TypeSystem system, Set<Type> types) {
        return this.findLub(system, types);
    }

    private Type findLub(TypeSystem system, Set<Type> types) {
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
                    if (!erasedCandidate.equals(otherCand) && this.typeCompatibilityChecker.isSubtype(erasedCandidate, otherCand)) {
                        permitted = false;
                        break;
                    }
                }

                if (permitted) {
                    minimalErasedCandidates.add(erasedCandidate);
                }
            }

            Map<ClassType, Set<ParameterizedClassType>> relevantParams = new LinkedHashMap<>();
            for (Type candidate : minimalErasedCandidates) {
                if (candidate instanceof ClassType ct && ct.hasTypeParameters()) {
                    for (Set<Type> supertypes : supertypeSets) {
                        for (Type supertype : supertypes) {
                            if (supertype instanceof ParameterizedClassType pct && ct.classReference().equals(pct.classReference())) {
                                relevantParams.computeIfAbsent(ct, k -> new LinkedHashSet<>())
                                        .add(pct);
                            }
                        }
                    }
                }
            }

            Set<Type> lub = new LinkedHashSet<>();
            for (Type candidate : minimalErasedCandidates) {
                if (relevantParams.containsKey(candidate)) {
                    lub.add(leastContainingParameterization(relevantParams.get(candidate)));
                } else {
                    lub.add(candidate);
                }
            }

            if (lub.isEmpty()) {
                return system.constants().nullType();
            } else if (lub.size() == 1) {
                return lub.iterator().next();
            } else {
                IntersectionType type = new IntersectionTypeImpl(system);
                type.setChildren(IntersectionType.flatten(lub));
                type.setUnmodifiable(true);
                return type;
            }
        }
    }

    private ParameterizedClassType leastContainingParameterization(Set<ParameterizedClassType> params) {
        if (params.size() == 1) {
            return leastContainingParameterization(params.iterator().next());
        }

        Iterator<ParameterizedClassType> iter = params.iterator();

        ParameterizedClassType curr = iter.next();
        while (iter.hasNext()) {
            ParameterizedClassType next = iter.next();
            curr = leastContainingParameterization(curr, next);
        }
        return curr;
    }

    private ParameterizedClassType leastContainingParameterization(ParameterizedClassType pct) {
        ParameterizedClassType lcta = new ParameterizedClassTypeImpl(pct.typeSystem());
        lcta.setClassReference(pct.classReference());

        if (pct.outerType() instanceof ParameterizedClassType outer) {
            lcta.setOuterType(leastContainingParameterization(outer));
        }

        List<ArgumentType> generated = new ArrayList<>();
        for (int i = 0; i < pct.typeArguments().size(); i++) {
            generated.add(singleLeastContainingTypeArgument(pct.typeArguments().get(i), pct.typeParameters().get(i)));
        }

        lcta.setTypeArguments(generated);
        lcta.setUnmodifiable(true);

        return lcta;
    }

    private ParameterizedClassType leastContainingParameterization(ParameterizedClassType left, ParameterizedClassType right) {
        ParameterizedClassType lcta = new ParameterizedClassTypeImpl(left.typeSystem());
        lcta.setClassReference(left.classReference());

        if (left.outerType() instanceof ParameterizedClassType lo && right.outerType() instanceof ParameterizedClassType ro) {
            lcta.setOuterType(leastContainingParameterization(lo, ro));
        }

        List<ArgumentType> generatedArguments = new ArrayList<>();
        for (int i = 0; i < Math.max(left.typeArguments().size(), right.typeArguments().size()); i++) {
            if (i < left.typeArguments().size() && i < right.typeArguments().size()) {
                generatedArguments.add(leastContainingTypeArgument(left.typeArguments().get(i), right.typeArguments().get(i)));
            } else if (i < left.typeArguments().size()) {
                generatedArguments.add(singleLeastContainingTypeArgument(left.typeArguments().get(i), left.typeParameters().get(i)));
            } else {
                generatedArguments.add(singleLeastContainingTypeArgument(right.typeArguments().get(i), right.typeParameters().get(i)));
            }
        }
        lcta.setTypeArguments(generatedArguments);
        lcta.setUnmodifiable(true);

        return lcta;
    }

    private ArgumentType singleLeastContainingTypeArgument(Type u, VarType corresponding) {
        if (u instanceof WildType.Upper wtu && wtu.hasDefaultBounds()) {
            return wildType(u.typeSystem());
        } else if (u instanceof VarType vt && vt.hasDefaultBounds()) {
            return wildType(u.typeSystem());
        } else if (corresponding.hasDefaultBounds()) {
            return wildType(u.typeSystem());
        }

        return this.lubWild(u.typeSystem(), u, u.typeSystem().constants().object());
    }

    private ArgumentType leastContainingTypeArgument(ArgumentType u, ArgumentType v) {
        if (u.equals(v)) return u;

        TypeSystem system = u.typeSystem();
        if (u instanceof WildType || v instanceof WildType) {
            WildType wild = (WildType) (u instanceof WildType ? u : v);
            Type other = u instanceof WildType ? v : u;

            if (wild instanceof WildType.Upper wtu) {
                if (other instanceof WildType.Upper owt) {
                    return lubWild(system, wtu.upperBounds(), owt.upperBounds());
                } else if (other instanceof WildType.Lower owl) {
                    return wildType(system);
                } else {
                    return lubWild(system, wtu.upperBounds(), v);
                }
            } else if (wild instanceof WildType.Lower wtl) {
                if (other instanceof WildType.Upper owt) {
                    return wildType(system);
                } else if (other instanceof WildType.Lower owl) {
                    return glbWild(system, wtl.lowerBounds(), owl.lowerBounds());
                } else {
                    return lubWild(system, wtl.lowerBound(), other);
                }
            } else {
                return lubWild(system, u, v);
            }
        } else {
            return lubWild(system, u, v);
        }
    }

    private WildType.Lower glbWild(TypeSystem system, Set<Type> left, Set<Type> right) {
        Set<Type> bounds = new LinkedHashSet<>();
        bounds.addAll(left);
        bounds.addAll(right);

        WildType.Lower lower = new WildTypeLowerImpl(system);
        lower.setLowerBounds(bounds);
        lower.setUnmodifiable(true);
        return lower;
    }

    private WildType.Lower glbWild(TypeSystem system, Set<Type> left, Type... types) {
        Set<Type> bounds = new LinkedHashSet<>();
        bounds.addAll(left);
        Collections.addAll(bounds, types);

        WildType.Lower lower = new WildTypeLowerImpl(system);
        lower.setLowerBounds(bounds);
        lower.setUnmodifiable(true);
        return lower;
    }

    private WildType.Upper wildType(TypeSystem system) {
        WildType.Upper wild = new WildTypeUpperImpl(system);
        wild.setUpperBounds(Set.of(system.constants().object()));
        wild.setUnmodifiable(true);
        return wild;
    }

    private WildType.Upper lubWild(TypeSystem system, Type... types) {
        WildType.Upper result = new WildTypeUpperImpl(system);
        result.setUpperBounds(Set.of(this.findLub(system, Set.of(types))));
        result.setUnmodifiable(true);
        return result;
    }

    private WildType.Upper lubWild(TypeSystem system, Set<Type> set, Type... types) {
        Set<Type> bounds = new LinkedHashSet<>(set);
        Collections.addAll(bounds, types);

        WildType.Upper result = new WildTypeUpperImpl(system);
        result.setUpperBounds(Set.of(this.findLub(system, bounds)));
        result.setUnmodifiable(true);
        return result;
    }

    private WildType.Upper lubWild(TypeSystem system, Set<Type> set, Set<Type> other) {
        Set<Type> bounds = new LinkedHashSet<>(set);
        bounds.addAll(other);

        WildType.Upper result = new WildTypeUpperImpl(system);
        result.setUpperBounds(Set.of(this.findLub(system, bounds)));
        result.setUnmodifiable(true);
        return result;
    }

    private static <T> Set<T> intersection(Collection<? extends Set<? extends T>> sets) {
        Set<T> result = new LinkedHashSet<>();
        sets.forEach(result::addAll);
        sets.forEach(result::retainAll);
        return result;
    }

    private static Set<Type> allSupertypes(Type type) {
        Set<Type> result = new LinkedHashSet<>();
        allSupertypes(type, result);
        return result;
    }

    private static void allSupertypes(Type type, Set<Type> types) {
        if (!types.contains(type)) {
            types.add(type);
            type.knownDirectSupertypes().forEach(t -> allSupertypes(t, types));
        }
    }

}
