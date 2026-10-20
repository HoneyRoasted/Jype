package honeyroasted.jype.system.solver.operations;

import honeyroasted.jype.metadata.JAccess;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.JMemberLookup;
import honeyroasted.jype.system.visitor.JTypeVisitors;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JType;

import java.lang.reflect.AccessFlag;

/**
 * Checks whether one method is a valid override of another per JLS §8.4.8.3. Both references
 * are expected to already be resolved into the overriding class's context (e.g. with any
 * inherited type variables substituted via the appropriate {@code varTypeResolver()}), since
 * this operation only checks the override rules themselves, not generic substitution.
 */
public class JCheckOverrideValidity implements JTypeOperation<JCheckOverrideValidity.Pair, JCheckOverrideValidity.Result> {

    public record Pair(JMethodReference override, JMethodReference overridden) {
    }

    public enum Reason {
        VALID,
        NOT_OVERRIDE_EQUIVALENT,
        HIDDEN_NOT_OVERRIDDEN,
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

        if (!isOverrideEquivalent(override, overridden)) {
            return Result.of(Reason.NOT_OVERRIDE_EQUIVALENT);
        }

        // Static methods hide, they don't override (JLS §8.4.8.2); only instance methods can
        // participate in the override rules checked here.
        if (override.hasModifier(AccessFlag.STATIC) || overridden.hasModifier(AccessFlag.STATIC)) {
            return Result.of(Reason.HIDDEN_NOT_OVERRIDDEN);
        }

        if (overridden.hasModifier(AccessFlag.PRIVATE)) {
            return Result.of(Reason.CANNOT_OVERRIDE_PRIVATE);
        }

        if (overridden.hasModifier(AccessFlag.FINAL)) {
            return Result.of(Reason.CANNOT_OVERRIDE_FINAL);
        }

        // JLS §8.4.8.3: the overriding method must provide at least as much access as the
        // overridden method (private < package-private < protected < public).
        JAccess overrideAccess = JAccess.fromFlags(override.modifiers());
        JAccess overriddenAccess = JAccess.fromFlags(overridden.modifiers());
        if (overrideAccess.ordinal() < overriddenAccess.ordinal()) {
            return Result.of(Reason.ACCESS_REDUCED);
        }

        if (!isReturnTypeSubstitutable(system, override.returnType(), overridden.returnType())) {
            return Result.of(Reason.RETURN_TYPE_NOT_SUBSTITUTABLE);
        }

        if (!isThrowsClauseCompatible(system, override, overridden)) {
            return Result.of(Reason.THROWS_CLAUSE_INCOMPATIBLE);
        }

        return Result.of(Reason.VALID);
    }

    /**
     * Two methods are override-equivalent when they share the same name and erased parameter
     * types (JLS §8.4.2).
     */
    private static boolean isOverrideEquivalent(JMethodReference a, JMethodReference b) {
        if (!a.location().name().equals(b.location().name())) return false;
        if (a.parameters().size() != b.parameters().size()) return false;

        JMethodReference ea = (JMethodReference) JTypeVisitors.ERASURE.visit(a);
        JMethodReference eb = (JMethodReference) JTypeVisitors.ERASURE.visit(b);
        for (int i = 0; i < ea.parameters().size(); i++) {
            if (!ea.parameters().get(i).typeEquals(eb.parameters().get(i))) return false;
        }
        return true;
    }

    /**
     * JLS §8.4.5: the overriding method's return type must be identical to, or (for reference
     * types) a subtype of, the overridden method's return type. Primitive and void returns must
     * match exactly - there is no covariance for them.
     */
    private static boolean isReturnTypeSubstitutable(JTypeSystem system, JType overrideReturn, JType overriddenReturn) {
        if (overrideReturn.typeEquals(overriddenReturn)) {
            return true;
        }

        boolean overriddenIsVoidOrPrimitive = overriddenReturn.typeEquals(system.constants().voidType())
                || overriddenReturn instanceof honeyroasted.jype.type.JPrimitiveType;
        if (overriddenIsVoidOrPrimitive) {
            return false;
        }

        return system.operations().isSubtype(overrideReturn, overriddenReturn);
    }

    /**
     * JLS §8.4.8.3: every checked exception type declared in the overriding method's throws
     * clause must be a subtype of (or the same as) some exception type declared in the
     * overridden method's throws clause. Unchecked exceptions (subtypes of RuntimeException or
     * Error) are unrestricted.
     */
    private static boolean isThrowsClauseCompatible(JTypeSystem system, JMethodReference override, JMethodReference overridden) {
        for (JType thrown : JMemberLookup.checkedExceptions(override)) {
            boolean covered = overridden.exceptionTypes().stream()
                    .anyMatch(declared -> thrown.typeEquals(declared) || system.operations().isSubtype(thrown, declared));
            if (!covered) {
                return false;
            }
        }
        return true;
    }
}
