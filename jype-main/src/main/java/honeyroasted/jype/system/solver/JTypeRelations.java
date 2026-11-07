package honeyroasted.jype.system.solver;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.system.JTypeConstants;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JTypeRelations {
    private static final Map<String, Set<String>> PRIMITIVE_SUPERTYPES = Map.of(
            "boolean", Set.of("boolean"),
            "byte", Set.of("byte", "short", "int", "long", "float", "double"),
            "short", Set.of("short", "int", "long", "float", "double"),
            "char", Set.of("char", "int", "long", "float", "double"),
            "int", Set.of("int", "long", "float", "double"),
            "long", Set.of("long", "float", "double"),
            "float", Set.of("float", "double"),
            "double", Set.of("double")
    );

    private static final ThreadLocal<Set<Pair<JType, JType>>> SUBTYPE_STACK = ThreadLocal.withInitial(HashSet::new);

    private JTypeRelations() {
    }

    public static boolean isSameType(JType s, JType t) {
        return s.typeEquals(t);
    }

    public static boolean isCompatible(JType s, JTypeConstraints.Compatible.Context context, JType t) {
        return switch (context) {
            case STRICT_INVOCATION -> isStrictInvocationCompatible(s, t);
            case LOOSE_INVOCATION, ASSIGNMENT -> isLooseInvocationCompatible(s, t);
            case EXPLICIT_CAST -> isCastable(s, t);
        };
    }

    public static boolean isStrictInvocationCompatible(JType s, JType t) {
        boolean sp = s instanceof JPrimitiveType;
        boolean tp = t instanceof JPrimitiveType;
        if (sp != tp) {
            return false;
        }
        // JLS §5.3 identity, widening primitive, widening reference, unchecked
        return isSubtypeUnchecked(s, t);
    }

    public static boolean isLooseInvocationCompatible(JType s, JType t) {
        boolean sp = s instanceof JPrimitiveType;
        boolean tp = t instanceof JPrimitiveType;
        if (sp == tp) {
            return isSubtypeUnchecked(s, t);
        } else if (sp) {
            // JLS §5.3 boxing then widening reference
            return isSubtype(((JPrimitiveType) s).box(), t);
        } else {
            // JLS §5.3 unboxing then widening primitive
            JPrimitiveType unboxed = unboxedType(s);
            return unboxed != null && isSubtype(unboxed, t);
        }
    }

    public static boolean isSubtypeUnchecked(JType s, JType t) {
        if (s instanceof JArrayType sa && t instanceof JArrayType ta) {
            if (sa.component() instanceof JPrimitiveType) {
                return isSameType(sa.component(), ta.component());
            }
            return isSubtypeUnchecked(sa.component(), ta.component());
        } else if (isSubtype(s, t)) {
            return true;
        } else if (s instanceof JVarType vt) {
            return isSubtypeUnchecked(vt.upperBound(), t);
        } else if (t instanceof JClassType tc && !isRaw(tc)) {
            // JLS §5.1.9 unchecked conversion
            JClassType sup = asSuper(s, tc.classReference());
            return sup != null && isRaw(sup);
        }
        return false;
    }

    public static boolean isSubtype(JType s, JType t) {
        Pair<JType, JType> key = Pair.identity(s, t);
        Set<Pair<JType, JType>> stack = SUBTYPE_STACK.get();
        if (!stack.add(key)) {
            return false;
        }
        try {
            return isSubtypeImpl(s, t);
        } finally {
            stack.remove(key);
        }
    }

    private static boolean isSubtypeImpl(JType s, JType t) {
        JTypeConstants c = s.typeSystem().constants();
        if (s == t || isSameType(s, t)) {
            return true;
        }
        if (s instanceof JMetaVarType || t instanceof JMetaVarType) {
            return false;
        }
        if (s instanceof JWildType || t instanceof JWildType) {
            return isWildcardSubtype(s, t);
        }
        if (s instanceof JNoneType sn) {
            // JLS §4.10.2 the null type is a subtype of every reference type
            return sn.isNullType() && !(t instanceof JPrimitiveType) && !(t instanceof JNoneType);
        }
        if (t instanceof JNoneType) {
            return false;
        }
        if (s instanceof JPrimitiveType sp) {
            // JLS §4.10.1
            return t instanceof JPrimitiveType tp && PRIMITIVE_SUPERTYPES.get(sp.name()).contains(tp.name());
        }
        if (t instanceof JPrimitiveType) {
            return false;
        }
        if (t instanceof JIntersectionType ti) {
            for (JType child : ti.children()) {
                if (!isSubtype(s, child)) {
                    return false;
                }
            }
            return true;
        }
        if (s instanceof JIntersectionType si) {
            // JLS §4.10.2 direct supertypes of an intersection are its components
            for (JType child : si.children()) {
                if (isSubtype(child, t)) {
                    return true;
                }
            }
            return false;
        }
        if (s instanceof JVarType sv) {
            // JLS §4.10.2 direct supertypes of a type variable are its bounds
            for (JType bound : upperBounds(sv)) {
                if (isSubtype(bound, t)) {
                    return true;
                }
            }
            if (t instanceof JVarType tv && tv.hasLowerBound()) {
                return isSubtype(s, tv.lowerBound());
            }
            return false;
        }
        if (t instanceof JVarType tv) {
            // JLS §4.10.2 a type variable with a lower bound is a supertype of it
            return tv.hasLowerBound() && isSubtype(s, tv.lowerBound());
        }
        if (t instanceof JArrayType ta) {
            if (s instanceof JArrayType sa) {
                // JLS §4.10.3
                if (sa.component() instanceof JPrimitiveType || ta.component() instanceof JPrimitiveType) {
                    return isSameType(sa.component(), ta.component());
                }
                return isSubtype(sa.component(), ta.component());
            }
            return false;
        }
        if (s instanceof JArrayType) {
            // JLS §4.10.3 Object, Cloneable and Serializable are supertypes of arrays
            return t instanceof JClassType tc && !tc.hasAnyTypeArguments() &&
                    (tc.classReference().typeEquals(c.object()) || tc.classReference().typeEquals(c.cloneable()) || tc.classReference().typeEquals(c.serializable()));
        }
        if (s instanceof JClassType sc && t instanceof JClassType tc) {
            return isClassSubtype(sc, tc);
        }
        return false;
    }

    private static boolean isWildcardSubtype(JType s, JType t) {
        if (t instanceof JWildType.Lower tl) {
            return isSubtype(s, tl.lowerBound());
        } else if (t instanceof JWildType) {
            return false;
        } else if (s instanceof JWildType.Upper su) {
            return isSubtype(su.upperBound(), t);
        } else {
            return isSubtype(s.typeSystem().constants().object(), t);
        }
    }

    private static boolean isClassSubtype(JClassType s, JClassType t) {
        JClassType source = hasWildcardArguments(s) ? capture(s) : s;
        JClassType sup = asSuper(source, t.classReference());
        if (sup == null) {
            return false;
        }
        if (!t.hasAnyTypeArguments() || isRaw(t)) {
            return true;
        }
        if (isRaw(sup)) {
            return false;
        }
        return argumentsContained(sup, t);
    }

    private static boolean argumentsContained(JClassType sup, JClassType t) {
        if (t.hasTypeArguments()) {
            if (!sup.hasTypeArguments() || sup.typeArguments().size() != t.typeArguments().size()) {
                return false;
            }
            for (int i = 0; i < t.typeArguments().size(); i++) {
                if (!contains(sup.typeArguments().get(i), t.typeArguments().get(i))) {
                    return false;
                }
            }
        }
        if (t.hasRelevantOuterType() && t.outerType() != null && t.outerType().hasAnyTypeArguments()) {
            JClassType so = sup.outerType();
            if (so == null) {
                return false;
            }
            JClassType soSuper = asSuper(hasWildcardArguments(so) ? capture(so) : so, t.outerType().classReference());
            return soSuper != null && !isRaw(soSuper) && argumentsContained(soSuper, t.outerType());
        }
        return true;
    }

    public static boolean contains(JType s, JType t) {
        JTypeConstants c = s.typeSystem().constants();
        if (t instanceof JWildType.Upper tu) {
            if (tu.hasDefaultBounds()) {
                // JLS §4.5.1 T <= ?
                return true;
            }
            JType upper = tu.upperBound();
            if (s instanceof JWildType.Upper su) {
                // JLS §4.5.1 ? extends T <= ? extends S if T <: S
                return isSubtype(su.hasDefaultBounds() ? c.object() : su.upperBound(), upper);
            } else if (s instanceof JWildType.Lower) {
                // JLS §4.5.1 ? super T <= ? extends Object
                return isSameType(upper, c.object());
            }
            // JLS §4.5.1 T <= ? extends T
            return isSubtype(s, upper);
        } else if (t instanceof JWildType.Lower tl) {
            JType lower = tl.lowerBound();
            if (s instanceof JWildType.Lower sl) {
                // JLS §4.5.1 ? super T <= ? super S if S <: T
                return isSubtype(lower, sl.lowerBound());
            } else if (s instanceof JWildType) {
                return false;
            }
            // JLS §4.5.1 T <= ? super T
            return isSubtype(lower, s);
        }
        if (s instanceof JWildType) {
            return false;
        }
        // JLS §4.5.1 T <= T
        return isSameType(s, t);
    }

    public static boolean hasWildcardArguments(JClassType type) {
        return type.hasTypeArguments() && type.typeArguments().stream().anyMatch(a -> a instanceof JWildType);
    }

    public static JClassType capture(JClassType type) {
        if (!(type instanceof JParameterizedClassType pct) || !hasWildcardArguments(type)) {
            return type;
        }
        JClassReference ref = pct.classReference();
        JTypeSystem system = type.typeSystem();
        List<JVarType> params = ref.typeParameters();
        List<JArgumentType> args = pct.typeArguments();
        if (params.size() != args.size()) {
            return type;
        }

        List<JArgumentType> captured = new ArrayList<>();
        Map<JVarType, JType> theta = new LinkedHashMap<>();
        List<JVarType> fresh = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            JArgumentType arg = args.get(i);
            if (arg instanceof JWildType) {
                JVarType z = JFreshTypeVariables.create(system, "CAP");
                z.metadata().attach(new JCapturedWildcard((JWildType) arg));
                fresh.add(z);
                captured.add(z);
                theta.put(params.get(i), z);
            } else {
                fresh.add(null);
                captured.add(arg);
                theta.put(params.get(i), arg);
            }
        }
        if (pct.hasRelevantOuterType() && pct.outerType() instanceof JParameterizedClassType outer) {
            theta.putAll(substitution(outer));
        }
        JVarTypeResolveVisitor resolver = new JVarTypeResolveVisitor(theta);

        for (int i = 0; i < args.size(); i++) {
            JVarType z = fresh.get(i);
            if (z == null) {
                continue;
            }
            Set<JType> declared = new LinkedHashSet<>();
            for (JType bound : params.get(i).upperBounds()) {
                declared.add(resolver.visit(bound));
            }
            declared.removeIf(b -> b.typeEquals(system.constants().object()));
            JWildType wild = (JWildType) args.get(i);
            Set<JType> upper = new LinkedHashSet<>();
            Set<JType> lower = new LinkedHashSet<>();
            if (wild instanceof JWildType.Upper wu) {
                if (!wu.hasDefaultBounds()) {
                    // JLS §5.1.10 ? extends Ui: upper glb(Bi θ, Ui)
                    upper.addAll(wu.upperBounds());
                }
                upper.addAll(declared);
            } else if (wild instanceof JWildType.Lower wl) {
                // JLS §5.1.10 ? super Bi: lower Bi, upper Ui θ
                upper.addAll(declared);
                lower.addAll(wl.lowerBounds());
            }
            if (upper.isEmpty()) {
                upper.add(system.constants().object());
            }
            JFreshTypeVariables.finish(z, upper, lower);
        }

        JParameterizedClassType result = ref.parameterized(captured);
        if (pct.hasRelevantOuterType() && pct.outerType() != null) {
            result.setUnmodifiable(false);
            result.setOuterType(pct.outerType());
            result.setUnmodifiable(true);
        }
        return result;
    }

    public static Set<JType> upperBounds(JVarType var) {
        if (var.upperBounds().isEmpty()) {
            return Set.of(var.typeSystem().constants().object());
        }
        return var.upperBounds();
    }

    public static boolean isRaw(JClassType type) {
        if (type.hasTypeArguments()) {
            return false;
        }
        if (type.classReference().hasTypeParameters()) {
            return true;
        }
        return type.hasRelevantOuterType() && type.outerType() != null && isRaw(type.outerType());
    }

    public static Map<JVarType, JType> substitution(JClassType type) {
        Map<JVarType, JType> map = new LinkedHashMap<>();
        if (type.hasRelevantOuterType() && type.outerType() != null) {
            map.putAll(substitution(type.outerType()));
        }
        if (type.hasTypeArguments()) {
            List<JVarType> params = type.classReference().typeParameters();
            List<JArgumentType> args = type.typeArguments();
            for (int i = 0; i < params.size() && i < args.size(); i++) {
                map.put(params.get(i), args.get(i));
            }
        }
        return map;
    }

    public static JClassType asSuper(JType s, JClassReference target) {
        return asSuper(s, target, new HashSet<>());
    }

    private static JClassType asSuper(JType s, JClassReference target, Set<JType> seen) {
        JTypeConstants c = target.typeSystem().constants();
        if (s instanceof JClassType sc) {
            return asSuperClass(sc, target, seen);
        } else if (s instanceof JVarType vt) {
            if (!seen.add(vt)) {
                return null;
            }
            for (JType bound : upperBounds(vt)) {
                JClassType r = asSuper(bound, target, seen);
                if (r != null) {
                    return r;
                }
            }
            return null;
        } else if (s instanceof JIntersectionType it) {
            for (JType child : it.children()) {
                JClassType r = asSuper(child, target, seen);
                if (r != null) {
                    return r;
                }
            }
            return null;
        } else if (s instanceof JArrayType) {
            if (target.typeEquals(c.object()) || target.typeEquals(c.cloneable()) || target.typeEquals(c.serializable())) {
                return target;
            }
            return null;
        } else if (s instanceof JWildType.Upper wu) {
            return asSuper(wu.upperBound(), target, seen);
        }
        return null;
    }

    private static JClassType asSuperClass(JClassType s, JClassReference target, Set<JType> seen) {
        if (s.classReference().typeEquals(target)) {
            return s;
        }
        boolean raw = isRaw(s);
        Map<JVarType, JType> theta = raw ? Map.of() : substitution(s);
        JVarTypeResolveVisitor resolver = theta.isEmpty() ? null : new JVarTypeResolveVisitor(theta);
        JClassReference ref = s.classReference();
        List<JClassType> supers = new ArrayList<>();
        if (ref.superClass() != null) {
            supers.add(ref.superClass());
        }
        supers.addAll(ref.interfaces());
        for (JClassType declared : supers) {
            JClassType inst;
            if (raw) {
                // JLS §4.8 supertypes of a raw type are the erasures of the supertypes
                inst = declared.classReference();
            } else if (resolver != null) {
                inst = (JClassType) resolver.visit(declared);
            } else {
                inst = declared;
            }
            JClassType r = asSuperClass(inst, target, seen);
            if (r != null) {
                return r;
            }
        }
        if (target.typeEquals(target.typeSystem().constants().object())) {
            return target;
        }
        return null;
    }

    public static List<JClassType> allSupertypeClasses(JType type) {
        List<JClassType> result = new ArrayList<>();
        collectSupertypeClasses(type, result, new HashSet<>());
        return result;
    }

    private static void collectSupertypeClasses(JType type, List<JClassType> result, Set<JType> seen) {
        if (!seen.add(type)) {
            return;
        }
        if (type instanceof JClassType ct) {
            if (result.stream().anyMatch(r -> r.typeEquals(ct))) {
                return;
            }
            result.add(ct);
            boolean raw = isRaw(ct);
            JVarTypeResolveVisitor resolver = new JVarTypeResolveVisitor(raw ? Map.of() : substitution(ct));
            JClassReference ref = ct.classReference();
            if (ref.superClass() != null) {
                collectSupertypeClasses(raw ? ref.superClass().classReference() : (JClassType) resolver.visit(ref.superClass()), result, seen);
            }
            for (JClassType iface : ref.interfaces()) {
                collectSupertypeClasses(raw ? iface.classReference() : (JClassType) resolver.visit(iface), result, seen);
            }
        } else if (type instanceof JVarType vt) {
            for (JType b : upperBounds(vt)) {
                collectSupertypeClasses(b, result, seen);
            }
        } else if (type instanceof JIntersectionType it) {
            for (JType c : it.children()) {
                collectSupertypeClasses(c, result, seen);
            }
        }
    }

    public static JPrimitiveType unboxedType(JType s) {
        JTypeConstants c = s.typeSystem().constants();
        for (JPrimitiveType prim : c.allPrimitives()) {
            JClassReference box = prim.box();
            if (box != null && asSuper(s, box) != null) {
                return prim;
            }
        }
        return null;
    }

    public static JType erasure(JType type) {
        if (type instanceof JClassType ct) {
            return ct.classReference();
        } else if (type instanceof JArrayType at) {
            JType comp = erasure(at.component());
            if (comp == at.component()) {
                return at;
            }
            JArrayType arr = type.typeSystem().typeFactory().newArrayType();
            arr.setComponent(comp);
            arr.setUnmodifiable(true);
            return arr;
        } else if (type instanceof JVarType vt) {
            return erasure(upperBounds(vt).iterator().next());
        } else if (type instanceof JIntersectionType it) {
            return erasure(it.children().iterator().next());
        } else if (type instanceof JWildType.Upper wu) {
            return erasure(wu.upperBound());
        } else if (type instanceof JWildType) {
            return type.typeSystem().constants().object();
        }
        return type;
    }

    public static boolean isCastable(JType s, JType t) {
        return JCastability.isCastable(s, t);
    }

    public static boolean isWellFormedGlb(Set<JType> types) {
        List<JType> flat = new ArrayList<>();
        for (JType t : types) {
            if (t instanceof JIntersectionType it) {
                flat.addAll(it.children());
            } else {
                flat.add(t);
            }
        }
        int classCount = 0;
        for (int i = 0; i < flat.size(); i++) {
            JType current = flat.get(i);
            boolean minimal = true;
            for (int j = 0; j < flat.size(); j++) {
                if (i != j) {
                    JType other = flat.get(j);
                    if (isSameType(other, current) ? j < i : isSubtype(other, current)) {
                        minimal = false;
                        break;
                    }
                }
            }
            if (minimal && !(current instanceof JClassType ct && ct.classReference().hasModifier(AccessFlag.INTERFACE))) {
                classCount++;
            }
        }
        // JLS §5.1.10 glb with more than one class bound is not well-formed
        return classCount <= 1;
    }

    public static boolean provablyDistinct(JType a, JType b) {
        boolean aVar = a instanceof JWildType || a instanceof JVarType;
        boolean bVar = b instanceof JWildType || b instanceof JVarType;
        if (!aVar && !bVar) {
            // JLS §4.5 neither is a type variable or wildcard, and not the same type
            return !isSameType(a, b);
        }
        JType au = erasure(argumentUpperBound(a));
        JType bu = erasure(argumentUpperBound(b));
        // JLS §4.5 upper bounds with unrelated erasures
        return !isSubtype(au, bu) && !isSubtype(bu, au);
    }

    private static JType argumentUpperBound(JType arg) {
        if (arg instanceof JWildType.Upper wu) {
            return wu.hasDefaultBounds() ? arg.typeSystem().constants().object() : wu.upperBound();
        } else if (arg instanceof JWildType) {
            return arg.typeSystem().constants().object();
        } else if (arg instanceof JVarType vt) {
            return vt.upperBound();
        }
        return arg;
    }
}
