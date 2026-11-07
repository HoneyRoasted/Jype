package honeyroasted.jype.system.solver.operations;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.system.JTypeConstants;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JFindLeastUpperBound implements JTypeOperation<Set<JType>, JType> {
    private static final ThreadLocal<Set<Pair<JType, JType>>> MERGE_CACHE = ThreadLocal.withInitial(HashSet::new);

    @Override
    public JType apply(JTypeSystem system, Set<JType> types) {
        return lub(system, new ArrayList<>(types));
    }

    private static JType lub(JTypeSystem system, List<JType> ts) {
        JTypeConstants c = system.constants();
        boolean anyArray = false;
        boolean anyClass = false;
        List<JType> filtered = new ArrayList<>();
        for (JType t : ts) {
            JType bound = t;
            while (bound instanceof JVarType vt) {
                bound = vt.upperBound();
            }
            if (t instanceof JPrimitiveType) {
                return c.noneType();
            } else if (t instanceof JNoneType) {
                continue;
            } else if (bound instanceof JArrayType) {
                anyArray = true;
            } else {
                anyClass = true;
            }
            filtered.add(t);
        }

        if (filtered.isEmpty()) {
            return c.nullType();
        }
        if (filtered.size() == 1) {
            return filtered.getFirst();
        }

        if (anyArray && !anyClass) {
            // JLS §4.10.4 lub of array types
            List<JType> elements = new ArrayList<>();
            for (JType t : filtered) {
                JType elem = elementType(t);
                if (elem instanceof JPrimitiveType) {
                    JType first = filtered.getFirst();
                    for (JType other : filtered) {
                        if (!JTypeRelations.isSameType(first, other)) {
                            return arraySuperType(system);
                        }
                    }
                    return first;
                }
                elements.add(elem);
            }
            JArrayType arr = system.typeFactory().newArrayType();
            arr.setComponent(lub(system, elements));
            arr.setUnmodifiable(true);
            return arr;
        } else if (anyArray) {
            List<JType> classes = new ArrayList<>();
            for (JType t : filtered) {
                JType bound = t;
                while (bound instanceof JVarType vt) {
                    bound = vt.upperBound();
                }
                if (!(bound instanceof JArrayType)) {
                    classes.add(t);
                }
            }
            classes.add(arraySuperType(system));
            return lub(system, classes);
        }

        // JLS §4.10.4 erased candidate set EC, minimal erased candidates MEC
        List<JType> ec = erasedSupertypes(filtered.getFirst());
        for (int i = 1; i < filtered.size(); i++) {
            List<JType> other = erasedSupertypes(filtered.get(i));
            List<JType> next = new ArrayList<>();
            for (JType e : ec) {
                if (other.stream().anyMatch(o -> JTypeRelations.isSameType(o, e))) {
                    next.add(e);
                }
            }
            ec = next;
        }
        List<JType> mec = closureMin(ec);

        List<JType> candidates = new ArrayList<>();
        for (JType erased : mec) {
            JType lci = null;
            boolean valid = true;
            for (JType t : filtered) {
                JType sup = asSuper(t, erased);
                if (sup == null) {
                    valid = false;
                    break;
                }
                lci = lci == null ? sup : intersect(system, lci, sup);
                if (lci == null) {
                    valid = false;
                    break;
                }
            }
            if (valid && lci != null) {
                candidates.add(lci);
            }
        }
        return compoundMin(system, candidates);
    }

    private static JType elementType(JType t) {
        while (t instanceof JVarType vt) {
            t = vt.upperBound();
        }
        return ((JArrayType) t).component();
    }

    private static JType arraySuperType(JTypeSystem system) {
        Set<JType> children = new LinkedHashSet<>();
        children.add(system.constants().serializable());
        children.add(system.constants().cloneable());
        JIntersectionType it = system.typeFactory().newIntersectionType();
        it.setChildren(children);
        it.setUnmodifiable(true);
        return it;
    }

    private static JType asSuper(JType t, JType erased) {
        if (erased instanceof JVarType) {
            return JTypeRelations.isSubtype(t, erased) ? erased : null;
        }
        return JTypeRelations.asSuper(t, ((JClassType) erased).classReference());
    }

    private static JType intersect(JTypeSystem system, JType a, JType b) {
        if (JTypeRelations.isSameType(a, b)) {
            return a;
        }
        if (a instanceof JClassType ac && b instanceof JClassType bc && ac.classReference().typeEquals(bc.classReference())) {
            if (ac.hasTypeArguments() && bc.hasTypeArguments()) {
                return merge(system, ac, bc);
            }
            if (JTypeRelations.isRaw(ac) || JTypeRelations.isRaw(bc)) {
                return ac.classReference();
            }
        }
        return null;
    }

    private static JType merge(JTypeSystem system, JClassType c1, JClassType c2) {
        List<JArgumentType> act1 = c1.typeArguments();
        List<JArgumentType> act2 = c2.typeArguments();
        List<JArgumentType> merged = new ArrayList<>();
        for (int i = 0; i < act1.size() && i < act2.size(); i++) {
            JArgumentType a1 = act1.get(i);
            JArgumentType a2 = act2.get(i);
            if (JTypeRelations.contains(a2, a1)) {
                merged.add(a1);
            } else if (JTypeRelations.contains(a1, a2)) {
                merged.add(a2);
            } else {
                Pair<JType, JType> pair = Pair.identity(c1, c2);
                Set<Pair<JType, JType>> cache = MERGE_CACHE.get();
                JWildType.Upper w = system.typeFactory().newUpperWildType();
                if (cache.add(pair)) {
                    try {
                        // JLS §4.10.4 lcta(U, V) = ? extends lub(U, V)
                        JType l = lub(system, List.of(wildUpperBound(a1), wildUpperBound(a2)));
                        w.setUpperBounds(l.typeEquals(system.constants().object()) ? new LinkedHashSet<>() : new LinkedHashSet<>(Set.of(l)));
                    } finally {
                        cache.remove(pair);
                    }
                } else {
                    w.setUpperBounds(new LinkedHashSet<>());
                }
                w.setUnmodifiable(true);
                merged.add(w);
            }
        }
        JParameterizedClassType result = c1.classReference().parameterized(merged);
        if (c1.hasRelevantOuterType() && c1.outerType() != null) {
            result.setUnmodifiable(false);
            result.setOuterType(c1.outerType());
            result.setUnmodifiable(true);
        }
        return result;
    }

    private static JType wildUpperBound(JType t) {
        if (t instanceof JWildType.Upper wu) {
            return wu.hasDefaultBounds() ? t.typeSystem().constants().object() : wildUpperBound(wu.upperBound());
        } else if (t instanceof JWildType) {
            return t.typeSystem().constants().object();
        }
        return t;
    }

    private static JType compoundMin(JTypeSystem system, List<JType> candidates) {
        if (candidates.isEmpty()) {
            return system.constants().object();
        }
        List<JType> min = closureMin(candidates);
        if (min.isEmpty()) {
            return system.constants().object();
        } else if (min.size() == 1) {
            return min.getFirst();
        }
        List<JType> classes = new ArrayList<>();
        List<JType> interfaces = new ArrayList<>();
        for (JType t : min) {
            if (t instanceof JClassType ct && ct.classReference().hasModifier(java.lang.reflect.AccessFlag.INTERFACE)) {
                interfaces.add(t);
            } else {
                classes.add(t);
            }
        }
        Set<JType> children = new LinkedHashSet<>(classes);
        children.addAll(interfaces);
        JIntersectionType it = system.typeFactory().newIntersectionType();
        it.setChildren(children);
        it.setUnmodifiable(true);
        return it;
    }

    private static List<JType> closureMin(List<JType> cl) {
        List<JType> result = new ArrayList<>();
        for (int i = 0; i < cl.size(); i++) {
            JType current = cl.get(i);
            boolean keep = true;
            for (int j = 0; j < cl.size(); j++) {
                if (i == j) {
                    continue;
                }
                JType other = cl.get(j);
                if (JTypeRelations.isSameType(other, current)) {
                    if (j < i) {
                        keep = false;
                        break;
                    }
                } else if (JTypeRelations.isSubtype(other, current)) {
                    keep = false;
                    break;
                }
            }
            if (keep) {
                result.add(current);
            }
        }
        return result;
    }

    private static List<JType> erasedSupertypes(JType t) {
        List<JType> result = new ArrayList<>();
        for (JType sup : closure(t)) {
            JType e = sup instanceof JVarType ? sup : JTypeRelations.erasure(sup);
            if (result.stream().noneMatch(r -> JTypeRelations.isSameType(r, e))) {
                result.add(e);
            }
        }
        return result;
    }

    private static List<JType> closure(JType t) {
        List<JType> result = new ArrayList<>();
        closure(t, result, new HashSet<>());
        return result;
    }

    private static void closure(JType t, List<JType> result, Set<JType> seen) {
        if (t instanceof JIntersectionType it) {
            for (JType child : it.children()) {
                closure(child, result, seen);
            }
            return;
        }
        if (result.stream().anyMatch(r -> JTypeRelations.isSameType(r, t))) {
            return;
        }
        result.add(t);
        if (t instanceof JVarType vt) {
            for (JType bound : JTypeRelations.upperBounds(vt)) {
                closure(bound, result, seen);
            }
        } else if (t instanceof JClassType ct) {
            JClassReference ref = ct.classReference();
            if (!seen.add(ref) && !ct.hasTypeArguments()) {
                return;
            }
            boolean raw = JTypeRelations.isRaw(ct);
            Map<JVarType, JType> theta = raw ? Map.of() : JTypeRelations.substitution(ct);
            JVarTypeResolveVisitor resolver = new JVarTypeResolveVisitor(theta);
            List<JClassType> supers = new ArrayList<>();
            if (ref.superClass() != null) {
                supers.add(ref.superClass());
            }
            supers.addAll(ref.interfaces());
            for (JClassType sup : supers) {
                closure(raw ? sup.classReference() : (JClassType) resolver.visit(sup), result, seen);
            }
            if (ref.superClass() == null && !ref.typeEquals(t.typeSystem().constants().object())) {
                closure(t.typeSystem().constants().object(), result, seen);
            }
        }
    }
}
