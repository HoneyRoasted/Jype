package honeyroasted.jype.system;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.solver.TypeSolver;
import honeyroasted.jype.system.solver.bounds.TypeBound;
import honeyroasted.jype.system.solver.bounds.TypeBoundCompoundUnwrapper;
import honeyroasted.jype.system.solver.bounds.TypeBoundMapperApplier;
import honeyroasted.jype.system.solver.solvers.NoOpTypeSolver;
import honeyroasted.jype.system.solver.solvers.TypeBoundMapperSolver;
import honeyroasted.jype.system.solver.solvers.compatibility.CompatibleExplicitCast;
import honeyroasted.jype.system.solver.solvers.compatibility.CompatibleLooseInvocation;
import honeyroasted.jype.system.solver.solvers.compatibility.CompatibleStrictInvocation;
import honeyroasted.jype.system.solver.solvers.compatibility.EqualType;
import honeyroasted.jype.system.solver.solvers.compatibility.ExpressionAssignmentConstant;
import honeyroasted.jype.system.solver.solvers.compatibility.ExpressionSimplyTyped;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeArray;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeEquality;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeGenericClass;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeIntersection;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeMetaVar;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeNone;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypePrimitive;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeRawClass;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeUnchecked;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeVar;
import honeyroasted.jype.system.solver.solvers.compatibility.SubtypeWild;
import honeyroasted.jype.system.visitor.TypeVisitors;
import honeyroasted.jype.type.ArgumentType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.MetaVarType;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeOperations {
    public static TypeBoundMapperApplier COMPATIBILITY_APPLIER = new TypeBoundMapperApplier(List.of(
            new TypeBoundCompoundUnwrapper(),

            new EqualType(),

            new CompatibleExplicitCast(),
            new CompatibleLooseInvocation(),
            new CompatibleStrictInvocation(),

            new ExpressionAssignmentConstant(),
            new ExpressionSimplyTyped(),

            new SubtypeNone(),
            new SubtypePrimitive(),
            new SubtypeEquality(),
            new SubtypeUnchecked(),
            new SubtypeRawClass(),
            new SubtypeGenericClass(),
            new SubtypeArray(),
            new SubtypeVar(),
            new SubtypeMetaVar(),
            new SubtypeWild(),
            new SubtypeIntersection()
    ));

    private TypeSystem typeSystem;

    public TypeOperations(TypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    public TypeSolver noOpSolver() {
        return new NoOpTypeSolver();
    }

    public TypeSolver compatibilitySolver() {
        return new TypeBoundMapperSolver("CompatibilityTypeSolver",
                Set.of(TypeBound.Equal.class, TypeBound.Subtype.class, TypeBound.Compatible.class),
                COMPATIBILITY_APPLIER);
    }

    public boolean isCompatible(Type subtype, Type supertype, TypeBound.Compatible.Context ctx) {
        return this.compatibilitySolver()
                .bind(new TypeBound.Compatible(subtype, supertype, ctx))
                .solve(this.typeSystem)
                .success();
    }

    public boolean isSubtype(Type subtype, Type supertype) {
        return this.compatibilitySolver()
                .bind(new TypeBound.Subtype(subtype, supertype))
                .solve(this.typeSystem)
                .success();
    }

    public Type findLeastUpperBound(TypeSystem system, Set<Type> types) {
        return this.findLub(system, types, new HashMap<>());
    }

    public Type findGreatestLowerBound(TypeSystem system, Set<Type> types) {
        return IntersectionType.of(types, system);
    }

    public Type findMostSpecificType(TypeSystem system, Set<Type> types) {
        return IntersectionType.of(findMostSpecificTypes(types), system);
    }

    public Set<Type> findMostSpecificTypes(Set<Type> types) {
        Set<Type> result = new LinkedHashSet<>();
        for (Type curr : types) {
            if (types.stream().noneMatch(t ->
                    this.isSubtype(t, curr))) {
                result.add(curr);
            }
        }
        return result;
    }

    public Set<Type> allKnownSupertypes(Type type) {
        Set<Type> result = new LinkedHashSet<>();
        allKnownSupertypes(type, result);
        return result;
    }

    private void allKnownSupertypes(Type type, Set<Type> building) {
        if (!building.contains(type)) {
            building.add(type);
            type.knownDirectSupertypes().forEach(t -> allSupertypes(t, building));
        }
    }

    public void updateMetaVars(Set<TypeBound.Result.Builder> bounds) {
        for (TypeBound.Result.Builder boundBuilder : bounds) {
            TypeBound bound = boundBuilder.bound();
            if (bound instanceof TypeBound.Equal eq) {
                if (eq.left() instanceof MetaVarType mvt) {
                    mvt.equalities().add(eq.right());
                }

                if (eq.right() instanceof MetaVarType mvt) {
                    mvt.equalities().add(eq.left());
                }
            }

            if (bound instanceof TypeBound.Subtype st) {
                if (st.left() instanceof MetaVarType mvt) {
                    mvt.upperBounds().add(st.right());
                }

                if (st.right() instanceof MetaVarType mvt) {
                    mvt.lowerBounds().add(st.right());
                }
            }
        }
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
                    if (!erasedCandidate.typeEquals(otherCand) && this.isSubtype(erasedCandidate, otherCand)) {
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

            IntersectionType type = new IntersectionTypeImpl(system);
            lubCache.put(key, type);

            Set<Type> lub = new LinkedHashSet<>();
            for (Type candidate : minimalErasedCandidates) {
                if (relevantParams.containsKey(candidate)) {
                    lub.add(leastContainingParameterization(lubCache, relevantParams.get(candidate)));
                } else {
                    lub.add(candidate);
                }
            }

            type.setChildren(IntersectionType.flatten(lub));
            type.setUnmodifiable(true);

            if (lub.isEmpty()) {
                return system.constants().nullType();
            } else if (lub.size() == 1) {
                return lub.iterator().next();
            } else {
                return type;
            }
        }
    }

    private ParameterizedClassType leastContainingParameterization(Map<Pair<Set<Type>, Set<Type>>, Type> lubCache, Set<ParameterizedClassType> params) {
        if (params.size() == 1) {
            return leastContainingParameterization(lubCache, params.iterator().next());
        }

        Iterator<ParameterizedClassType> iter = params.iterator();

        ParameterizedClassType curr = iter.next();
        while (iter.hasNext()) {
            ParameterizedClassType next = iter.next();
            curr = leastContainingParameterization(lubCache, curr, next);
        }
        return curr;
    }

    private ParameterizedClassType leastContainingParameterization(Map<Pair<Set<Type>, Set<Type>>, Type> lubCache, ParameterizedClassType pct) {
        ParameterizedClassType lcta = new ParameterizedClassTypeImpl(pct.typeSystem());
        lcta.setClassReference(pct.classReference());

        if (pct.outerType() instanceof ParameterizedClassType outer) {
            lcta.setOuterType(leastContainingParameterization(lubCache, outer));
        }

        List<ArgumentType> generated = new ArrayList<>();
        for (int i = 0; i < pct.typeArguments().size(); i++) {
            generated.add(singleLeastContainingTypeArgument(lubCache, pct.typeArguments().get(i), pct.typeParameters().get(i)));
        }

        lcta.setTypeArguments(generated);
        lcta.setUnmodifiable(true);

        return lcta;
    }

    private ParameterizedClassType leastContainingParameterization(Map<Pair<Set<Type>, Set<Type>>, Type> lubCache, ParameterizedClassType left, ParameterizedClassType right) {
        ParameterizedClassType lcta = new ParameterizedClassTypeImpl(left.typeSystem());
        lcta.setClassReference(left.classReference());

        if (left.outerType() instanceof ParameterizedClassType lo && right.outerType() instanceof ParameterizedClassType ro) {
            lcta.setOuterType(leastContainingParameterization(lubCache, lo, ro));
        }

        List<ArgumentType> generatedArguments = new ArrayList<>();
        for (int i = 0; i < Math.max(left.typeArguments().size(), right.typeArguments().size()); i++) {
            if (i < left.typeArguments().size() && i < right.typeArguments().size()) {
                generatedArguments.add(leastContainingTypeArgument(lubCache, left.typeArguments().get(i), right.typeArguments().get(i)));
            } else if (i < left.typeArguments().size()) {
                generatedArguments.add(singleLeastContainingTypeArgument(lubCache, left.typeArguments().get(i), left.typeParameters().get(i)));
            } else {
                generatedArguments.add(singleLeastContainingTypeArgument(lubCache, right.typeArguments().get(i), right.typeParameters().get(i)));
            }
        }
        lcta.setTypeArguments(generatedArguments);
        lcta.setUnmodifiable(true);

        return lcta;
    }

    private ArgumentType singleLeastContainingTypeArgument(Map<Pair<Set<Type>, Set<Type>>, Type> lubCache, Type u, VarType corresponding) {
        if (u instanceof WildType.Upper wtu && wtu.hasDefaultBounds()) {
            return wildType(u.typeSystem());
        } else if (u instanceof VarType vt && vt.hasDefaultBounds()) {
            return wildType(u.typeSystem());
        } else if (corresponding.hasDefaultBounds()) {
            return wildType(u.typeSystem());
        }

        return this.lubWild(lubCache, u.typeSystem(), u, u.typeSystem().constants().object());
    }

    private ArgumentType leastContainingTypeArgument(Map<Pair<Set<Type>, Set<Type>>, Type> lubCache, ArgumentType u, ArgumentType v) {
        if (u.typeEquals(v)) return u;

        TypeSystem system = u.typeSystem();
        if (u instanceof WildType || v instanceof WildType) {
            WildType wild = (WildType) (u instanceof WildType ? u : v);
            Type other = u instanceof WildType ? v : u;

            if (wild instanceof WildType.Upper wtu) {
                if (other instanceof WildType.Upper owt) {
                    return lubWild(lubCache, system, wtu.upperBounds(), owt.upperBounds());
                } else if (other instanceof WildType.Lower owl) {
                    return wildType(system);
                } else {
                    return lubWild(lubCache, system, wtu.upperBounds(), v);
                }
            } else if (wild instanceof WildType.Lower wtl) {
                if (other instanceof WildType.Upper owt) {
                    return wildType(system);
                } else if (other instanceof WildType.Lower owl) {
                    return glbWild(system, wtl.lowerBounds(), owl.lowerBounds());
                } else {
                    return lubWild(lubCache, system, wtl.lowerBound(), other);
                }
            } else {
                return lubWild(lubCache, system, u, v);
            }
        } else {
            return lubWild(lubCache, system, u, v);
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

    private WildType.Upper wildType(TypeSystem system) {
        WildType.Upper wild = new WildTypeUpperImpl(system);
        wild.setUpperBounds(Set.of(system.constants().object()));
        wild.setUnmodifiable(true);
        return wild;
    }

    private WildType.Upper lubWild(Map<Pair<Set<Type>, Set<Type>>, Type> lubCache, TypeSystem system, Type... types) {
        WildType.Upper result = new WildTypeUpperImpl(system);
        result.setUpperBounds(Set.of(this.findLub(system, Set.of(types), lubCache)));
        result.setUnmodifiable(true);
        return result;
    }

    private WildType.Upper lubWild(Map<Pair<Set<Type>, Set<Type>>, Type> lubCache, TypeSystem system, Set<Type> set, Type... types) {
        Set<Type> bounds = new LinkedHashSet<>(set);
        Collections.addAll(bounds, types);

        WildType.Upper result = new WildTypeUpperImpl(system);
        result.setUpperBounds(Set.of(this.findLub(system, bounds, lubCache)));
        result.setUnmodifiable(true);
        return result;
    }

    private WildType.Upper lubWild(Map<Pair<Set<Type>, Set<Type>>, Type> lubCache, TypeSystem system, Set<Type> set, Set<Type> other) {
        Set<Type> bounds = new LinkedHashSet<>(set);
        bounds.addAll(other);

        WildType.Upper result = new WildTypeUpperImpl(system);
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
