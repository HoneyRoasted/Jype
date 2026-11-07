package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JFieldReference;
import honeyroasted.jype.type.JType;

import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JFieldLookup {
    private JFieldLookup() {
    }

    public record Found(JFieldReference field, JType type) {
    }

    public static Optional<JFieldReference> member(JClassReference cls, String name) {
        List<JFieldReference> found = new ArrayList<>();
        collect(cls, name, found);
        if (found.size() == 1) {
            return Optional.of(found.getFirst());
        }
        return Optional.empty();
    }

    private static void collect(JClassReference cls, String name, List<JFieldReference> found) {
        for (JFieldReference f : cls.declaredFields()) {
            if (f.location().name().equals(name)) {
                // JLS §8.3 a declared field hides inherited fields of the same name
                addDistinct(found, f);
                return;
            }
        }
        List<JClassType> supers = new ArrayList<>();
        if (cls.superClass() != null) {
            supers.add(cls.superClass());
        }
        supers.addAll(cls.interfaces());
        for (JClassType sup : supers) {
            List<JFieldReference> inherited = new ArrayList<>();
            collect(sup.classReference(), name, inherited);
            for (JFieldReference f : inherited) {
                if (!f.hasModifier(AccessFlag.PRIVATE)) {
                    // JLS §8.3.3 a class may inherit more than one field with the same name
                    addDistinct(found, f);
                }
            }
        }
    }

    private static void addDistinct(List<JFieldReference> found, JFieldReference f) {
        if (found.stream().noneMatch(e -> e.location().equals(f.location()))) {
            found.add(f);
        }
    }

    public static Optional<Found> staticAccess(JClassReference cls, String name, JClassReference declaring) {
        Optional<JFieldReference> field = member(cls, name);
        if (field.isEmpty() || !field.get().hasModifier(AccessFlag.STATIC) || !accessible(field.get(), declaring, null)) {
            return Optional.empty();
        }
        return Optional.of(new Found(field.get(), captured(field.get().type())));
    }

    public static Optional<Found> instanceAccess(JType receiver, String name, JClassReference declaring) {
        if (receiver instanceof JArrayType && name.equals("length")) {
            // JLS §10.7 the public final field length
            return Optional.of(new Found(null, receiver.typeSystem().constants().intType()));
        }
        JType site = receiver instanceof JClassType ct ? JTypeRelations.capture(ct) : receiver;
        for (JClassType ct : JMemberLookup.findClassTypes(site)) {
            Optional<JFieldReference> field = member(ct.classReference(), name);
            if (field.isPresent()) {
                JFieldReference f = field.get();
                if (!accessible(f, declaring, ct)) {
                    return Optional.empty();
                }
                return Optional.of(new Found(f, captured(memberType(ct, f))));
            }
        }
        return Optional.empty();
    }

    public static JType memberType(JClassType site, JFieldReference field) {
        if (field.hasModifier(AccessFlag.STATIC)) {
            return field.type();
        }
        JClassType sup = JTypeRelations.asSuper(site, field.outerClass());
        if (sup == null) {
            return field.type();
        }
        if (JTypeRelations.isRaw(sup)) {
            // JLS §4.8 the type of a field of a raw type is its erasure
            return JTypeRelations.erasure(field.type());
        }
        return new JVarTypeResolveVisitor(JTypeRelations.substitution(sup)).visit(field.type());
    }

    private static boolean accessible(JFieldReference field, JClassReference declaring, JClassType receiver) {
        if (declaring == null) {
            return true;
        }
        return field.outerClass().accessFrom(declaring, receiver).canAccess(field.access());
    }

    private static JType captured(JType type) {
        // JLS §6.5.6.1 the type of a field access is the captured type
        return type instanceof JClassType ct && type.isProperType() ? JTypeRelations.capture(ct) : type;
    }
}
