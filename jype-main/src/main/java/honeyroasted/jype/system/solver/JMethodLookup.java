package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.visitor.JTypeVisitors;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class JMethodLookup {
    private JMethodLookup() {
    }

    public record Member(JMethodReference declaration, JMethodReference method, JClassType declaringType) {
    }

    public static List<Member> members(JType site, String name) {
        JTypeSystem system = site.typeSystem();
        List<JClassType> supertypes = new ArrayList<>();
        if (site instanceof JArrayType) {
            supertypes.add(system.constants().object());
        } else {
            supertypes.addAll(JTypeRelations.allSupertypeClasses(site instanceof JClassType ct && JTypeRelations.hasWildcardArguments(ct) ? JTypeRelations.capture(ct) : site));
            if (supertypes.stream().noneMatch(s -> s.classReference().typeEquals(system.constants().object()))) {
                // JLS §9.2 interfaces implicitly declare the public methods of Object
                supertypes.add(system.constants().object());
            }
        }
        JClassReference siteClass = site instanceof JClassType sct ? sct.classReference() : null;

        List<Member> result = new ArrayList<>();
        for (JClassType sup : supertypes) {
            JClassReference ref = sup.classReference();
            boolean own = siteClass != null && ref.typeEquals(siteClass);
            boolean objectFromInterface = siteClass != null && siteClass.hasModifier(AccessFlag.INTERFACE) && ref.typeEquals(system.constants().object());
            for (JMethodReference m : ref.declaredMethods()) {
                if (!m.location().name().equals(name) || m.location().isConstructor() || m.hasModifier(AccessFlag.BRIDGE) || m.hasModifier(AccessFlag.SYNTHETIC)) {
                    continue;
                }
                if (!own && m.hasModifier(AccessFlag.PRIVATE)) {
                    // JLS §8.4.8 private methods are not inherited
                    continue;
                }
                if (!own && m.hasModifier(AccessFlag.STATIC) && ref.hasModifier(AccessFlag.INTERFACE)) {
                    // JLS §8.4.8 static interface methods are not inherited
                    continue;
                }
                if (objectFromInterface && (!m.hasModifier(AccessFlag.PUBLIC) || m.hasModifier(AccessFlag.STATIC))) {
                    continue;
                }
                JMethodReference member = asMemberOf(sup, m, site);
                if (overridden(result, member, sup)) {
                    continue;
                }
                result.add(new Member(m, member, sup));
            }
        }
        return result;
    }

    public static JMethodReference asMemberOf(JClassType declaringType, JMethodReference m, JType site) {
        if (site instanceof JArrayType && m.location().name().equals("clone") && m.parameters().isEmpty()) {
            // JLS §10.7 clone of an array type returns the array type
            JMethodReference copy = m.copy();
            copy.setUnmodifiable(false);
            copy.setReturnType(site);
            copy.setExceptionTypes(new ArrayList<>());
            copy.setModifiers((m.modifiers() & ~java.lang.reflect.Modifier.PROTECTED) | java.lang.reflect.Modifier.PUBLIC);
            copy.setUnmodifiable(true);
            return copy;
        }
        if (m.hasModifier(AccessFlag.STATIC)) {
            return m;
        }
        if (JTypeRelations.isRaw(declaringType)) {
            // JLS §4.8 members of a raw type have erased signatures
            return (JMethodReference) JTypeVisitors.ERASURE.visit(m);
        }
        if (!declaringType.hasAnyTypeArguments()) {
            return m;
        }
        return (JMethodReference) new JVarTypeResolveVisitor(JTypeRelations.substitution(declaringType)).visit(m);
    }

    private static boolean overridden(List<Member> existing, JMethodReference candidate, JClassType candidateOwner) {
        for (Member e : existing) {
            if (e.method().parameters().size() == candidate.parameters().size()
                    && JTypeRelations.isSubtype(e.declaringType().classReference(), candidateOwner.classReference())
                    && !e.declaringType().classReference().typeEquals(candidateOwner.classReference())
                    && isSubsignature(e.method(), candidate)) {
                // JLS §8.4.8.1 the inherited method is overridden or hidden
                return true;
            }
        }
        return false;
    }

    public static boolean isSubsignature(JMethodReference m1, JMethodReference m2) {
        if (sameSignature(m1, m2)) {
            return true;
        }
        // JLS §8.4.2 m1 has the same signature as the erasure of m2
        JMethodReference e2 = (JMethodReference) JTypeVisitors.ERASURE.visit(m2);
        return m1.typeParameters().isEmpty() && sameTypes(m1.parameters(), e2.parameters(), Map.of());
    }

    public static boolean sameSignature(JMethodReference m1, JMethodReference m2) {
        Optional<Map<JVarType, JType>> adaptation = adaptation(m1, m2);
        // JLS §8.4.2 same type parameters (§8.4.4) and same formal parameter types after adaptation
        return adaptation.isPresent() && sameTypes(m1.parameters(), m2.parameters(), adaptation.get());
    }

    public static Optional<Map<JVarType, JType>> adaptation(JMethodReference m1, JMethodReference m2) {
        List<JVarType> p1 = m1.typeParameters();
        List<JVarType> p2 = m2.typeParameters();
        if (p1.size() != p2.size()) {
            return Optional.empty();
        }
        Map<JVarType, JType> theta = new LinkedHashMap<>();
        for (int i = 0; i < p1.size(); i++) {
            theta.put(p2.get(i), p1.get(i));
        }
        JVarTypeResolveVisitor resolver = new JVarTypeResolveVisitor(theta);
        for (int i = 0; i < p1.size(); i++) {
            // JLS §8.4.4 the bounds of corresponding type parameters are the same after renaming
            if (!JTypeRelations.isSameType(p1.get(i).upperBound(), resolver.visit(p2.get(i).upperBound()))) {
                return Optional.empty();
            }
        }
        return Optional.of(theta);
    }

    private static boolean sameTypes(List<JType> a, List<JType> b, Map<JVarType, JType> theta) {
        if (a.size() != b.size()) {
            return false;
        }
        JVarTypeResolveVisitor resolver = new JVarTypeResolveVisitor(theta);
        for (int i = 0; i < a.size(); i++) {
            if (!JTypeRelations.isSameType(a.get(i), theta.isEmpty() ? b.get(i) : resolver.visit(b.get(i)))) {
                return false;
            }
        }
        return true;
    }

    public static JType getClassReturnType(JType receiver) {
        JTypeSystem system = receiver.typeSystem();
        JClassReference clazz = system.tryResolve(Class.class);
        JWildType.Upper w = system.typeFactory().newUpperWildType();
        JType erased = JTypeRelations.erasure(receiver);
        w.setUpperBounds(new LinkedHashSet<>(Set.of(erased)));
        w.setUnmodifiable(true);
        // JLS §4.3.2 getClass returns Class<? extends |T|>
        return clazz.parameterized(List.of((JArgumentType) w));
    }
}
