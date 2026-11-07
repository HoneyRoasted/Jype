package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.system.visitor.JTypeVisitors;
import honeyroasted.jype.system.visitor.visitors.JRecursiveTypeVisitor;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class JFunctionalInterfaces {
    private JFunctionalInterfaces() {
    }

    private record Abstract(JMethodReference method, JClassType owner) {
    }

    public static Optional<JMethodReference> functionType(JType target) {
        List<JClassType> roots = new ArrayList<>();
        if (target instanceof JIntersectionType it) {
            for (JType child : it.children()) {
                if (!(child instanceof JClassType ct)) {
                    return Optional.empty();
                }
                roots.add(ct);
            }
        } else if (target instanceof JClassType ct) {
            roots.add(ct);
        } else {
            return Optional.empty();
        }
        JTypeSystem system = target.typeSystem();
        boolean anyInterface = false;
        for (JClassType root : roots) {
            if (root.classReference().hasModifier(AccessFlag.INTERFACE)) {
                anyInterface = true;
            } else if (!root.classReference().typeEquals(system.constants().object())) {
                // JLS §9.8 a functional interface type must be an interface (or an intersection of interfaces)
                return Optional.empty();
            }
        }
        if (!anyInterface) {
            return Optional.empty();
        }

        List<Abstract> methods = new ArrayList<>();
        Set<JClassReference> seen = new LinkedHashSet<>();
        for (JClassType root : roots) {
            if (!root.classReference().hasModifier(AccessFlag.INTERFACE)) {
                continue;
            }
            for (JClassType sup : JTypeRelations.allSupertypeClasses(root)) {
                if (!sup.classReference().hasModifier(AccessFlag.INTERFACE) || !seen.add(sup.classReference())) {
                    continue;
                }
                for (JMethodReference m : sup.classReference().declaredMethods()) {
                    if (!m.hasModifier(AccessFlag.ABSTRACT) || m.hasModifier(AccessFlag.STATIC) || m.hasModifier(AccessFlag.PRIVATE)) {
                        continue;
                    }
                    if (isPublicObjectMethod(system, m)) {
                        // JLS §9.8 methods matching public Object methods do not count
                        continue;
                    }
                    methods.add(new Abstract(memberOf(sup, m), sup));
                }
            }
        }

        List<Abstract> remaining = new ArrayList<>();
        for (Abstract a : methods) {
            boolean overridden = false;
            for (Abstract b : methods) {
                if (a != b && !a.owner().classReference().typeEquals(b.owner().classReference())
                        && JTypeRelations.isSubtype(b.owner().classReference(), a.owner().classReference())
                        && JMethodLookup.isSubsignature(b.method(), a.method())) {
                    overridden = true;
                    break;
                }
            }
            if (!overridden) {
                remaining.add(a);
            }
        }
        if (remaining.isEmpty()) {
            return Optional.empty();
        }

        for (Abstract candidate : remaining) {
            boolean ok = true;
            for (Abstract other : remaining) {
                if (candidate == other) {
                    continue;
                }
                if (!candidate.method().location().name().equals(other.method().location().name())
                        || !JMethodLookup.isSubsignature(candidate.method(), other.method())
                        || !returnTypeSubstitutable(candidate.method(), other.method())) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                // JLS §9.8 a single abstract method, or override-equivalent abstract methods merged into one
                return Optional.of(candidate.method());
            }
        }
        return Optional.empty();
    }

    private static boolean returnTypeSubstitutable(JMethodReference m1, JMethodReference m2) {
        JType r1 = m1.returnType();
        JType r2 = m2.returnType();
        JTypeSystem system = r1.typeSystem();
        if (r1.typeEquals(system.constants().voidType())) {
            return r2.typeEquals(system.constants().voidType());
        }
        if (r1 instanceof JPrimitiveType) {
            return r1.typeEquals(r2);
        }
        // JLS §8.4.5 return-type-substitutable
        return JTypeRelations.isSubtype(r1, r2) || JTypeRelations.isSubtypeUnchecked(r1, r2) || JTypeRelations.isSameType(JTypeRelations.erasure(r1), JTypeRelations.erasure(r2));
    }

    private static JMethodReference memberOf(JClassType owner, JMethodReference m) {
        if (JTypeRelations.isRaw(owner)) {
            return (JMethodReference) JTypeVisitors.ERASURE.visit(m);
        }
        if (!owner.hasAnyTypeArguments()) {
            return m;
        }
        return (JMethodReference) new JVarTypeResolveVisitor(JTypeRelations.substitution(owner)).visit(m);
    }

    private static boolean isPublicObjectMethod(JTypeSystem system, JMethodReference m) {
        for (JMethodReference om : system.constants().object().declaredMethods()) {
            if (om.hasModifier(AccessFlag.PUBLIC) && om.location().name().equals(m.location().name()) && om.parameters().size() == m.parameters().size()) {
                boolean same = true;
                for (int i = 0; i < om.parameters().size(); i++) {
                    if (!JTypeRelations.isSameType(JTypeRelations.erasure(m.parameters().get(i)), om.parameters().get(i))) {
                        same = false;
                        break;
                    }
                }
                if (same) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Optional<JType> nonWildcardParameterization(JType target) {
        if (target instanceof JIntersectionType it) {
            List<JType> children = new ArrayList<>();
            for (JType child : it.children()) {
                Optional<JType> c = nonWildcardParameterization(child);
                if (c.isEmpty()) {
                    return Optional.empty();
                }
                children.add(c.get());
            }
            return Optional.of(JIntersectionType.of(new LinkedHashSet<>(children), target.typeSystem()));
        }
        if (!(target instanceof JParameterizedClassType pct) || !JTypeRelations.hasWildcardArguments(pct)) {
            return Optional.of(target);
        }
        JClassReference ref = pct.classReference();
        List<JVarType> params = ref.typeParameters();
        List<JArgumentType> args = pct.typeArguments();
        List<JArgumentType> result = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            JArgumentType a = args.get(i);
            JVarType p = params.get(i);
            if (!(a instanceof JWildType)) {
                result.add(a);
                continue;
            }
            Set<JType> bounds = JTypeRelations.upperBounds(p);
            for (JType b : bounds) {
                if (mentionsAny(b, params)) {
                    // JLS §9.9 a wildcard whose type parameter bound mentions the parameters has no function type
                    return Optional.empty();
                }
            }
            JType bi = bounds.size() == 1 ? bounds.iterator().next() : JIntersectionType.of(bounds, target.typeSystem());
            if (a instanceof JWildType.Upper wu && wu.hasDefaultBounds()) {
                // JLS §9.9 ? becomes Bi
                result.add((JArgumentType) bi);
            } else if (a instanceof JWildType.Upper wu) {
                // JLS §9.9 ? extends Ui becomes glb(Ui, Bi)
                Set<JType> glb = new LinkedHashSet<>(wu.upperBounds());
                if (!bi.typeEquals(target.typeSystem().constants().object())) {
                    glb.add(bi);
                }
                result.add((JArgumentType) target.typeSystem().operations().findGreatestLowerBound(glb));
            } else {
                // JLS §9.9 ? super Li becomes Li
                result.add((JArgumentType) ((JWildType.Lower) a).lowerBound());
            }
        }
        JParameterizedClassType out = ref.parameterized(result);
        if (pct.hasRelevantOuterType() && pct.outerType() != null) {
            out.setUnmodifiable(false);
            out.setOuterType(pct.outerType());
            out.setUnmodifiable(true);
        }
        return Optional.of(out);
    }

    private static boolean mentionsAny(JType type, List<JVarType> vars) {
        return !new JRecursiveTypeVisitor<JType, Void>((JTypeVisitor.Default) (t, c) -> t instanceof JVarType vt && vars.contains(vt) ? vt : null, null, false)
                .visit(type, new HashMap<>()).isEmpty();
    }
}
