package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.metadata.JAccess;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.JMemberLookup;
import honeyroasted.jype.system.solver.JMethodLookup;
import honeyroasted.jype.system.solver.JTypeRelations;
import honeyroasted.jype.system.visitor.JTypeVisitors;
import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.lang.reflect.AccessFlag;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class JCheckOverrideValidity implements JTypeOperation<JCheckOverrideValidity.Pair, JCheckOverrideValidity.Result> {

    public record Pair(JMethodReference override, JMethodReference overridden) {
    }

    public enum Reason {
        VALID,
        NOT_OVERRIDE_EQUIVALENT,
        NAME_CLASH,
        NOT_INHERITED,
        STATIC_HIDES_INSTANCE,
        INSTANCE_OVERRIDES_STATIC,
        CANNOT_OVERRIDE_PRIVATE,
        CANNOT_OVERRIDE_FINAL,
        ACCESS_REDUCED,
        RETURN_TYPE_NOT_SUBSTITUTABLE,
        THROWS_CLAUSE_INCOMPATIBLE
    }

    public record Result(boolean valid, Reason reason) {
        public static Result of(Reason reason) {
            return new Result(reason == Reason.VALID, reason);
        }
    }

    @Override
    public Result apply(JTypeSystem system, Pair pair) {
        JMethodReference override = pair.override();
        JMethodReference overridden = pair.overridden();

        if (!override.location().name().equals(overridden.location().name())) {
            return Result.of(Reason.NOT_OVERRIDE_EQUIVALENT);
        }
        if (overridden.hasModifier(AccessFlag.PRIVATE)) {
            // JLS §8.4.8 private methods are not inherited
            return Result.of(Reason.CANNOT_OVERRIDE_PRIVATE);
        }
        if (JAccess.fromFlags(overridden.modifiers()) == JAccess.PACKAGE_PROTECTED && !samePackage(override, overridden)) {
            // JLS §8.4.8.1 a package-access method is only overridden from the same package
            return Result.of(Reason.NOT_INHERITED);
        }
        if (!JMethodLookup.isSubsignature(override, overridden)) {
            JMethodReference eo = (JMethodReference) JTypeVisitors.ERASURE.visit(override);
            JMethodReference ed = (JMethodReference) JTypeVisitors.ERASURE.visit(overridden);
            if (sameErasedParameters(eo.parameters(), ed.parameters())) {
                // JLS §8.4.8.3 same erasure without either overriding the other
                return Result.of(Reason.NAME_CLASH);
            }
            return Result.of(Reason.NOT_OVERRIDE_EQUIVALENT);
        }

        boolean overrideStatic = override.hasModifier(AccessFlag.STATIC);
        boolean overriddenStatic = overridden.hasModifier(AccessFlag.STATIC);
        if (overrideStatic && !overriddenStatic) {
            // JLS §8.4.8.2 a static method cannot hide an instance method
            return Result.of(Reason.STATIC_HIDES_INSTANCE);
        }
        if (!overrideStatic && overriddenStatic) {
            // JLS §8.4.8.1 an instance method cannot override a static method
            return Result.of(Reason.INSTANCE_OVERRIDES_STATIC);
        }
        if (overridden.hasModifier(AccessFlag.FINAL)) {
            // JLS §8.4.3.3 a final method cannot be overridden or hidden
            return Result.of(Reason.CANNOT_OVERRIDE_FINAL);
        }

        JAccess overrideAccess = JAccess.fromFlags(override.modifiers());
        JAccess overriddenAccess = JAccess.fromFlags(overridden.modifiers());
        if (overrideAccess.ordinal() < overriddenAccess.ordinal()) {
            // JLS §8.4.8.3 at least as much access
            return Result.of(Reason.ACCESS_REDUCED);
        }

        Map<JVarType, JType> theta = adaptation(override, overridden);
        if (!isReturnTypeSubstitutable(system, override, overridden, theta)) {
            return Result.of(Reason.RETURN_TYPE_NOT_SUBSTITUTABLE);
        }
        if (!isThrowsClauseCompatible(system, override, overridden, theta)) {
            return Result.of(Reason.THROWS_CLAUSE_INCOMPATIBLE);
        }
        return Result.of(Reason.VALID);
    }

    private static boolean samePackage(JMethodReference a, JMethodReference b) {
        return Objects.equals(a.outerClass().namespace().location().getPackage(), b.outerClass().namespace().location().getPackage());
    }

    private static boolean sameErasedParameters(List<JType> a, List<JType> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!JTypeRelations.isSameType(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static Map<JVarType, JType> adaptation(JMethodReference override, JMethodReference overridden) {
        Optional<Map<JVarType, JType>> adapt = JMethodLookup.adaptation(overridden, override);
        return adapt.orElseGet(LinkedHashMap::new);
    }

    private static boolean isReturnTypeSubstitutable(JTypeSystem system, JMethodReference override, JMethodReference overridden, Map<JVarType, JType> theta) {
        JType r1 = new JVarTypeResolveVisitor(theta).visit(override.returnType());
        JType r2 = overridden.returnType();
        if (r2.typeEquals(system.constants().voidType()) || r1.typeEquals(system.constants().voidType())) {
            // JLS §8.4.5 void only matches void
            return r1.typeEquals(r2);
        }
        if (r1 instanceof JPrimitiveType || r2 instanceof JPrimitiveType) {
            // JLS §8.4.5 primitive return types are identical
            return r1.typeEquals(r2);
        }
        if (JTypeRelations.isSubtype(r1, r2) || JTypeRelations.isSubtypeUnchecked(r1, r2)) {
            // JLS §8.4.5 R1 adapted is a subtype of R2, or converts to one by unchecked conversion
            return true;
        }
        // JLS §8.4.5 d1 does not have the same signature as d2 and R1 = |R2|
        return !JMethodLookup.sameSignature(override, overridden) && JTypeRelations.isSameType(r1, JTypeRelations.erasure(r2));
    }

    private static boolean isThrowsClauseCompatible(JTypeSystem system, JMethodReference override, JMethodReference overridden, Map<JVarType, JType> theta) {
        boolean erased = !JMethodLookup.sameSignature(override, overridden);
        JVarTypeResolveVisitor resolver = new JVarTypeResolveVisitor(theta);
        List<JType> declared = overridden.exceptionTypes().stream().map(t -> erased ? JTypeRelations.erasure(t) : t).toList();
        for (JType thrown : JMemberLookup.checkedExceptions(override)) {
            JType adapted = resolver.visit(thrown);
            // JLS §8.4.8.3 every checked exception is a subtype of some exception the overridden method declares
            if (declared.stream().noneMatch(d -> JTypeRelations.isSubtype(adapted, d))) {
                return false;
            }
        }
        return true;
    }
}
