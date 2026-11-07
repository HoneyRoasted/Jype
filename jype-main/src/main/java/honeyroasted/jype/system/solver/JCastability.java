package honeyroasted.jype.system.solver;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.system.JTypeConstants;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JRecursiveTypeVisitor;
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

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JCastability {
    private static final ThreadLocal<Set<Pair<JType, JType>>> DISJOINT_CACHE = ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<Set<Pair<JType, JType>>> CAST_CACHE = ThreadLocal.withInitial(HashSet::new);

    private final JRelationTracker tracker;

    private JCastability(JRelationTracker tracker) {
        this.tracker = tracker;
    }

    public static boolean isCastable(JType t, JType s) {
        return isCastable(t, s, null);
    }

    public static boolean isCastable(JType t, JType s, JRelationTracker tracker) {
        return new JCastability(tracker).castable(t, s);
    }

    public static JClassType asSub(JClassType t, JClassReference sym) {
        return new JCastability(null).asSubtype(t, sym);
    }

    private boolean castable(JType t, JType s) {
        if (this.tracker == null) {
            return this.castableImpl(t, s);
        }
        this.tracker.enter(new JTypeConstraints.Compatible(t, JTypeConstraints.Compatible.Context.EXPLICIT_CAST, s));
        boolean result = false;
        try {
            result = this.castableImpl(t, s);
            return result;
        } finally {
            this.tracker.exit(result);
        }
    }

    private boolean sub(JType s, JType t) {
        return JTypeRelations.isSubtype(s, t, this.tracker);
    }

    private boolean subUnchecked(JType s, JType t) {
        return JTypeRelations.isSubtypeUnchecked(s, t, this.tracker);
    }

    private boolean loose(JType s, JType t) {
        return JTypeRelations.isLooseInvocationCompatible(s, t, this.tracker);
    }

    private void rule(String rule) {
        if (this.tracker != null) {
            this.tracker.rule(rule);
        }
    }

    private boolean castableImpl(JType t, JType s) {
        if (t == s || JTypeRelations.isSameType(t, s)) {
            return true;
        }
        boolean tp = t instanceof JPrimitiveType;
        boolean sp = s instanceof JPrimitiveType;
        if (tp != sp) {
            // JLS §5.5 boxing/unboxing casting contexts
            JType skipped = skipTypeVars(t);
            if (skipped instanceof JNoneType && !skipped.isNullType()) {
                return false;
            }
            return this.loose(skipped, s) || (sp && !(skipped instanceof JNoneType) && this.sub(((JPrimitiveType) s).box(), skipped));
        }
        boolean result = visit(t, s);
        if (result && t instanceof JClassType tc && s instanceof JClassType sc && (tc.classReference().isSealed() || sc.classReference().isSealed())) {
            // JLS §5.1.6.1 sealed hierarchies make some casts impossible
            return !areDisjoint(tc.classReference(), sc.classReference(), new HashSet<>());
        }
        return result;
    }

    private boolean areDisjoint(JClassReference ts, JClassReference ss, Set<Pair<JType, JType>> seen) {
        if (!seen.add(Pair.identity(ts, ss))) {
            return false;
        }
        boolean tInterface = ts.hasModifier(AccessFlag.INTERFACE);
        boolean sInterface = ss.hasModifier(AccessFlag.INTERFACE);
        if (tInterface != sInterface) {
            JClassReference isym = tInterface ? ts : ss;
            JClassReference csym = isym == ts ? ss : ts;
            if (!this.sub(csym, isym)) {
                if (csym.hasModifier(AccessFlag.FINAL)) {
                    return true;
                } else if (csym.isSealed()) {
                    return csym.permittedSubclasses().stream().allMatch(p -> areDisjoint(isym, p, seen));
                } else if (isym.isSealed()) {
                    return isym.permittedSubclasses().stream().allMatch(p -> areDisjoint(csym, p, seen));
                }
            }
        } else if (!tInterface) {
            return !this.sub(ss, ts) && !this.sub(ts, ss);
        } else if (!this.sub(ts, ss) && !this.sub(ss, ts)) {
            if (ts.isSealed()) {
                return ts.permittedSubclasses().stream().allMatch(p -> areDisjoint(ss, p, seen));
            } else if (ss.isSealed()) {
                return ss.permittedSubclasses().stream().allMatch(p -> areDisjoint(ts, p, seen));
            }
        }
        return false;
    }

    private boolean visit(JType t, JType s) {
        JTypeConstants c = t.typeSystem().constants();
        if (t instanceof JPrimitiveType) {
            // JLS §5.5 table 5.5-A
            boolean tb = t.typeEquals(c.booleanType());
            boolean sb = s.typeEquals(c.booleanType());
            return tb == sb;
        }
        if (t instanceof JNoneType tn) {
            return tn.isNullType() && this.sub(t, s);
        }
        if (s instanceof JNoneType sn) {
            return sn.isNullType();
        }
        if (t instanceof JWildType) {
            return this.castable(wildUpperBound(t), s);
        }
        if (t instanceof JClassType || t instanceof JIntersectionType) {
            return visitClass(t, s);
        }
        if (t instanceof JArrayType ta) {
            if (s instanceof JVarType) {
                return this.castable(s, t);
            } else if (s instanceof JClassType || s instanceof JIntersectionType) {
                return this.sub(t, s);
            } else if (s instanceof JArrayType sa) {
                if (ta.component() instanceof JPrimitiveType || sa.component() instanceof JPrimitiveType) {
                    return ta.component().typeEquals(sa.component());
                }
                return this.castable(ta.component(), sa.component());
            }
            return false;
        }
        if (t instanceof JVarType tv) {
            if (s instanceof JVarType) {
                return this.sub(t, s) || this.castable(tv.upperBound(), s);
            }
            return this.castable(tv.upperBound(), s);
        }
        return false;
    }

    private boolean visitClass(JType t, JType s) {
        if (s instanceof JVarType sv) {
            return this.castable(t, sv.upperBound());
        }
        if (t instanceof JIntersectionType || s instanceof JIntersectionType) {
            return !(t instanceof JIntersectionType) ? visitCompound((JIntersectionType) s, t, true) : visitCompound((JIntersectionType) t, s, false);
        }
        JClassType tc = (JClassType) t;
        if (s instanceof JClassType || s instanceof JArrayType) {
            JType et = JTypeRelations.erasure(t);
            JType es = JTypeRelations.erasure(s);
            boolean upcast = this.sub(et, es);
            if (upcast || this.sub(es, et)) {
                if (!upcast && s instanceof JArrayType) {
                    return true;
                } else if (s instanceof JClassType sc && JTypeRelations.isRaw(sc)) {
                    return true;
                } else if (JTypeRelations.isRaw(tc)) {
                    return true;
                }
                JClassType a = (JClassType) (upcast ? t : s);
                JClassType b = (JClassType) (upcast ? s : t);
                Boolean quantified = castByQuantifiers(a, b, false);
                if (quantified == null) {
                    quantified = castByQuantifiers(a, b, true);
                }
                if (quantified != null && quantified) {
                    return true;
                }
                return this.subUnchecked(a, b);
            }
            if (s instanceof JClassType sc) {
                boolean sInterface = sc.classReference().hasModifier(AccessFlag.INTERFACE);
                boolean tInterface = tc.classReference().hasModifier(AccessFlag.INTERFACE);
                if (sInterface) {
                    return !tc.classReference().hasModifier(AccessFlag.FINAL) ? sideCast(tc, sc) : sideCastFinal(tc, sc);
                } else if (tInterface) {
                    return !sc.classReference().hasModifier(AccessFlag.FINAL) ? sideCast(tc, sc) : sideCastFinal(tc, sc);
                }
                return false;
            }
        }
        return false;
    }

    private Boolean castByQuantifiers(JClassType a, JClassType b, boolean rewriteTypeVars) {
        JClassType aHigh = (JClassType) rewriteQuantifiers(a, true, rewriteTypeVars);
        JClassType aLow = (JClassType) rewriteQuantifiers(a, false, rewriteTypeVars);
        JClassType bHigh = (JClassType) rewriteQuantifiers(b, true, rewriteTypeVars);
        JClassType bLow = (JClassType) rewriteQuantifiers(b, false, rewriteTypeVars);
        JClassType lowSub = this.asSubtype(bLow, aLow.classReference());
        JClassType highSub = lowSub == null ? null : this.asSubtype(bHigh, aHigh.classReference());
        if (highSub == null) {
            return null;
        }
        return !disjointTypes(allParams(aHigh), allParams(highSub))
                && !disjointTypes(allParams(aHigh), allParams(lowSub))
                && !disjointTypes(allParams(aLow), allParams(highSub))
                && !disjointTypes(allParams(aLow), allParams(lowSub));
    }

    private boolean visitCompound(JIntersectionType ct, JType s, boolean reverse) {
        for (JType c : ct.children()) {
            if (reverse ? !this.castable(s, c) : !this.castable(c, s)) {
                return false;
            }
        }
        return true;
    }

    private boolean sideCast(JClassType from, JClassType to) {
        if (!to.classReference().hasModifier(AccessFlag.INTERFACE)) {
            JClassType tmp = to;
            to = from;
            from = tmp;
        }
        List<JClassType> commonSupers = superClosure(to, JTypeRelations.erasure(from));
        for (JClassType sup : commonSupers) {
            JClassType t1 = JTypeRelations.asSuper(from, sup.classReference());
            if (t1 != null && disjointTypes(t1.typeArguments(), sup.typeArguments())) {
                return false;
            }
        }
        return true;
    }

    private boolean sideCastFinal(JClassType from, JClassType to) {
        if (!to.classReference().hasModifier(AccessFlag.INTERFACE)) {
            JClassType tmp = to;
            to = from;
            from = tmp;
        }
        JClassType t1 = JTypeRelations.asSuper(from, to.classReference());
        if (t1 == null) {
            return false;
        }
        return !disjointTypes(t1.typeArguments(), to.typeArguments());
    }

    private List<JClassType> superClosure(JClassType t, JType s) {
        List<JClassType> result = new ArrayList<>();
        for (JClassType iface : interfaces(t)) {
            if (this.sub(s, JTypeRelations.erasure(iface))) {
                if (result.stream().noneMatch(r -> r.typeEquals(iface))) {
                    result.add(iface);
                }
            } else {
                for (JClassType r : superClosure(iface, s)) {
                    if (result.stream().noneMatch(x -> x.typeEquals(r))) {
                        result.add(r);
                    }
                }
            }
        }
        return result;
    }

    private static List<JClassType> interfaces(JClassType t) {
        List<JClassType> result = new ArrayList<>();
        boolean raw = JTypeRelations.isRaw(t);
        JVarTypeResolveVisitor resolver = new JVarTypeResolveVisitor(raw ? Map.of() : JTypeRelations.substitution(t));
        for (JClassType iface : t.classReference().interfaces()) {
            result.add(raw ? iface.classReference() : (JClassType) resolver.visit(iface));
        }
        return result;
    }

    private boolean disjointTypes(List<? extends JType> ts, List<? extends JType> ss) {
        for (int i = 0; i < ts.size() && i < ss.size(); i++) {
            if (disjointType(ts.get(i), ss.get(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean disjointType(JType t, JType s) {
        if (s instanceof JWildType) {
            return disjointWildcard((JWildType) s, t);
        } else if (t instanceof JWildType tw) {
            return disjointWildcard(tw, s);
        }
        return notSoftSubtypeRecursive(t, s) || notSoftSubtypeRecursive(s, t);
    }

    private boolean disjointWildcard(JWildType t, JType s) {
        if (isUnbound(t)) {
            return false;
        }
        if (!(s instanceof JWildType sw)) {
            if (t instanceof JWildType.Upper tu) {
                return notSoftSubtypeRecursive(s, tu.upperBound());
            }
            return notSoftSubtypeRecursive(((JWildType.Lower) t).lowerBound(), s);
        }
        if (isUnbound(sw)) {
            return false;
        }
        if (t instanceof JWildType.Upper tu) {
            if (sw instanceof JWildType.Upper) {
                return !isCastableRecursive(tu.upperBound(), wildUpperBound(sw));
            }
            return notSoftSubtypeRecursive(wildLowerBound(sw), tu.upperBound());
        }
        if (sw instanceof JWildType.Upper) {
            return notSoftSubtypeRecursive(((JWildType.Lower) t).lowerBound(), wildUpperBound(sw));
        }
        return false;
    }

    private boolean isCastableRecursive(JType t, JType s) {
        Pair<JType, JType> key = Pair.identity(t, s);
        Set<Pair<JType, JType>> cache = CAST_CACHE.get();
        if (!cache.add(key)) {
            return true;
        }
        try {
            return this.castable(t, s);
        } finally {
            cache.remove(key);
        }
    }

    private boolean notSoftSubtypeRecursive(JType t, JType s) {
        Pair<JType, JType> key = Pair.identity(t, s);
        Set<Pair<JType, JType>> cache = DISJOINT_CACHE.get();
        if (!cache.add(key)) {
            return false;
        }
        try {
            return notSoftSubtype(t, s);
        } finally {
            cache.remove(key);
        }
    }

    private boolean notSoftSubtype(JType t, JType s) {
        if (t == s) {
            return false;
        }
        if (t instanceof JVarType tv) {
            return !this.castable(tv.upperBound(), relaxBound(s));
        }
        if (!(s instanceof JWildType)) {
            s = cvarUpperBound(s);
        }
        return !this.sub(t, relaxBound(s));
    }

    private static JType relaxBound(JType t) {
        return t instanceof JVarType ? rewriteQuantifiers(skipTypeVars(t), true, true) : t;
    }

    private static boolean isCaptured(JType t) {
        return t instanceof JVarType vt && vt.metadata().has(JCapturedWildcard.class);
    }

    private static JType cvarUpperBound(JType t) {
        while (isCaptured(t)) {
            t = ((JVarType) t).upperBound();
        }
        return t;
    }

    private static JType skipTypeVars(JType t) {
        while (t instanceof JVarType vt) {
            t = vt.upperBound();
        }
        return t;
    }

    private static boolean isUnbound(JWildType w) {
        if (w instanceof JWildType.Upper wu) {
            return wu.hasDefaultBounds();
        }
        return false;
    }

    private static JType wildUpperBound(JType t) {
        JTypeConstants c = t.typeSystem().constants();
        if (t instanceof JWildType.Upper wu) {
            return wu.hasDefaultBounds() ? c.object() : wildUpperBound(wu.upperBound());
        } else if (t instanceof JWildType.Lower) {
            return c.object();
        }
        return t;
    }

    private static JType wildLowerBound(JType t) {
        if (t instanceof JWildType.Upper) {
            return t.typeSystem().constants().nullType();
        } else if (t instanceof JWildType.Lower wl) {
            return wildLowerBound(wl.lowerBound());
        }
        return t;
    }

    private static List<JType> allParams(JClassType t) {
        List<JType> result = new ArrayList<>();
        if (t.hasRelevantOuterType() && t.outerType() != null) {
            result.addAll(allParams(t.outerType()));
        }
        result.addAll(t.typeArguments());
        return result;
    }

    private static JParameterizedClassType declaredType(JClassReference ref) {
        return ref.parameterizedWithTypeVars();
    }

    private JClassType asSubtype(JClassType t, JClassReference sym) {
        if (t.classReference().typeEquals(sym)) {
            return t;
        }
        JParameterizedClassType symType = declaredType(sym);
        JClassType base = JTypeRelations.asSuper(symType, t.classReference());
        if (base == null) {
            return null;
        }
        Map<JVarType, JType> mapping = new LinkedHashMap<>();
        try {
            adapt(base, t, mapping, new HashSet<>());
        } catch (AdaptFailure e) {
            return null;
        }
        JClassType res = (JClassType) new JVarTypeResolveVisitor(mapping).visit(symType);
        if (!this.sub(res, t)) {
            return null;
        }
        List<JVarType> openVars = new ArrayList<>();
        for (JVarType p : sym.typeParameters()) {
            if (mentions(res, p) && !mentions(t, p)) {
                openVars.add(p);
            }
        }
        if (!openVars.isEmpty()) {
            if (JTypeRelations.isRaw(t)) {
                return res.classReference();
            }
            Map<JVarType, JType> opens = new HashMap<>();
            for (JVarType v : openVars) {
                opens.put(v, unboundedWildcard(t.typeSystem()));
            }
            res = (JClassType) new JVarTypeResolveVisitor(opens).visit(res);
        }
        return res;
    }

    private static boolean mentions(JType type, JVarType var) {
        List<JType> found = new JRecursiveTypeVisitor<JType, Void>((JTypeVisitor.Default) (tt, ctx) -> tt instanceof JVarType vt && vt.typeEquals(var) ? vt : null, null, false).visit(type, new HashMap<>());
        return !found.isEmpty();
    }

    private static final class AdaptFailure extends RuntimeException {
        private AdaptFailure() {
            super(null, null, false, false);
        }
    }

    private void adapt(JType source, JType target, Map<JVarType, JType> mapping, Set<Pair<JType, JType>> cache) {
        if (!cache.add(Pair.identity(source, target))) {
            return;
        }
        try {
            if (source instanceof JClassType sc) {
                if (target instanceof JClassType tc) {
                    List<JType> sp = allParams(sc);
                    List<JType> tp = allParams(tc);
                    if (sp.size() == tp.size()) {
                        for (int i = 0; i < sp.size(); i++) {
                            adapt(sp.get(i), tp.get(i), mapping, cache);
                        }
                    }
                }
            } else if (source instanceof JArrayType sa) {
                if (target instanceof JArrayType ta) {
                    adapt(sa.component(), ta.component(), mapping, cache);
                }
            } else if (source instanceof JWildType sw) {
                if (isExtendsBound(sw)) {
                    adapt(wildUpperBound(sw), wildUpperBound(target), mapping, cache);
                } else if (isSuperBound(sw)) {
                    adapt(wildLowerBound(sw), wildLowerBound(target), mapping, cache);
                }
            } else if (source instanceof JVarType sv) {
                JType val = mapping.get(sv);
                if (val != null) {
                    if (isSuperBound(val) && isSuperBound(target)) {
                        val = this.sub(wildLowerBound(val), wildLowerBound(target)) ? target : val;
                    } else if (isExtendsBound(val) && isExtendsBound(target)) {
                        val = this.sub(wildUpperBound(val), wildUpperBound(target)) ? val : target;
                    } else if (!JTypeRelations.isSameType(val, target)) {
                        throw new AdaptFailure();
                    }
                } else {
                    val = target;
                }
                mapping.put(sv, val);
            }
        } finally {
            cache.remove(Pair.identity(source, target));
        }
    }

    private static boolean isSuperBound(JType t) {
        return t instanceof JWildType.Lower || (t instanceof JWildType.Upper wu && wu.hasDefaultBounds());
    }

    private static boolean isExtendsBound(JType t) {
        return t instanceof JWildType.Upper;
    }

    public static JType rewriteQuantifiers(JType t, boolean high, boolean rewriteTypeVars) {
        return new Rewriter(high, rewriteTypeVars).visit(t);
    }

    private static JWildType unboundedWildcard(JTypeSystem system) {
        JWildType.Upper w = system.typeFactory().newUpperWildType();
        w.setUpperBounds(new LinkedHashSet<>());
        w.setUnmodifiable(true);
        return w;
    }

    private static final class Rewriter {
        private final boolean high;
        private final boolean rewriteTypeVars;
        private final Map<JType, JType> argMap = new HashMap<>();
        private final Set<JType> seen = new HashSet<>();

        private Rewriter(boolean high, boolean rewriteTypeVars) {
            this.high = high;
            this.rewriteTypeVars = rewriteTypeVars;
        }

        private JType visit(JType t) {
            if (t instanceof JClassType ct) {
                return this.visitClass(ct);
            } else if (t instanceof JVarType vt) {
                return this.visitVar(vt);
            } else if (t instanceof JWildType wt) {
                return this.visitWildcard(wt);
            }
            return t;
        }

        private JType visitClass(JClassType t) {
            if (!t.hasTypeArguments() && !(t.hasRelevantOuterType() && t.outerType() != null && t.outerType().hasAnyTypeArguments())) {
                return t;
            }
            boolean changed = false;
            JClassType outer = t.hasRelevantOuterType() ? t.outerType() : null;
            JClassType newOuter = outer;
            if (outer != null && outer.hasAnyTypeArguments()) {
                newOuter = (JClassType) this.visitClass(outer);
                changed = newOuter != outer;
            }
            List<JArgumentType> args = new ArrayList<>();
            for (JArgumentType arg : t.typeArguments()) {
                JType bound = this.argMap.get(arg);
                if (bound == null) {
                    bound = this.visit(arg);
                    this.argMap.put(arg, bound);
                }
                if (bound != arg) {
                    changed = true;
                }
                args.add((JArgumentType) bound);
            }
            if (!changed) {
                return t;
            }
            JParameterizedClassType result = t.classReference().parameterized(args);
            if (newOuter != null) {
                result.setUnmodifiable(false);
                result.setOuterType(newOuter);
                result.setUnmodifiable(true);
            }
            return result;
        }

        private JType visitVar(JVarType t) {
            if (isCaptured(t)) {
                JWildType w = t.metadata().first(JCapturedWildcard.class).get().wildcard();
                boolean extendsKind = w instanceof JWildType.Upper;
                if (w instanceof JWildType.Upper wu && wu.hasDefaultBounds()) {
                    return unboundedWildcard(t.typeSystem());
                }
                JType wBound = extendsKind ? ((JWildType.Upper) w).upperBound() : ((JWildType.Lower) w).lowerBound();
                JType bound = mentions(wBound, t) ? JTypeRelations.erasure(wBound) : this.visit(wBound);
                return this.rewriteAsWildcard(this.visit(bound), extendsKind);
            }
            if (this.seen.add(t)) {
                if (this.rewriteTypeVars) {
                    JType upper = t.upperBound();
                    JType bound = mentions(upper, t) ? JTypeRelations.erasure(upper) : this.visit(upper);
                    return this.rewriteAsWildcard(bound, true);
                }
                return t;
            }
            return this.rewriteTypeVars ? unboundedWildcard(t.typeSystem()) : t;
        }

        private JType visitWildcard(JWildType t) {
            if (t instanceof JWildType.Upper wu) {
                if (wu.hasDefaultBounds()) {
                    return t;
                }
                JType b = wu.upperBound();
                JType b2 = this.visit(b);
                return b == b2 ? t : this.rewriteAsWildcard(b2, true);
            }
            JWildType.Lower wl = (JWildType.Lower) t;
            JType b = wl.lowerBound();
            JType b2 = this.visit(b);
            return b == b2 ? t : this.rewriteAsWildcard(b2, false);
        }

        private JType rewriteAsWildcard(JType bound, boolean extendsKind) {
            JTypeSystem system = bound.typeSystem();
            if (extendsKind) {
                return this.high ? makeExtends(this.b(bound)) : unboundedWildcard(system);
            }
            return this.high ? unboundedWildcard(system) : makeSuper(this.b(bound));
        }

        private JType b(JType t) {
            while (t instanceof JWildType w) {
                if (this.high) {
                    t = w instanceof JWildType.Upper wu && !wu.hasDefaultBounds() ? wu.upperBound() : t.typeSystem().constants().object();
                } else {
                    t = w instanceof JWildType.Lower wl ? wl.lowerBound() : t.typeSystem().constants().nullType();
                }
            }
            return t;
        }

        private static JType makeExtends(JType bound) {
            JTypeSystem system = bound.typeSystem();
            if (bound.typeEquals(system.constants().object())) {
                return unboundedWildcard(system);
            }
            JWildType.Upper w = system.typeFactory().newUpperWildType();
            w.setUpperBounds(new LinkedHashSet<>(Set.of(bound)));
            w.setUnmodifiable(true);
            return w;
        }

        private static JType makeSuper(JType bound) {
            JTypeSystem system = bound.typeSystem();
            if (bound.isNullType()) {
                return unboundedWildcard(system);
            }
            JWildType.Lower w = system.typeFactory().newLowerWildType();
            w.setLowerBounds(new LinkedHashSet<>(Set.of(bound)));
            w.setUnmodifiable(true);
            return w;
        }
    }
}
