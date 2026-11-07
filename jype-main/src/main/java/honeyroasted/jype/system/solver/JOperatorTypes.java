package honeyroasted.jype.system.solver;

import honeyroasted.jype.system.JTypeConstants;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;

import java.util.Optional;

/**
 * Shared helpers for the numeric/boolean rules operator expressions (JLS §15.15/§15.17-§15.24) and
 * conditional expressions (JLS §15.25) are built from. Kept separate from those two reducers
 * because, despite both needing "unbox and promote" logic, the actual promotion tables differ:
 * binary numeric promotion (§5.6.2, used by arithmetic/relational/bitwise operators) has no
 * byte/short-special-case or constant-narrowing rule, while conditional expression numeric typing
 * (§15.25) does - so only the unboxing/rank plumbing is shared, not a single "promote" entrypoint.
 */
public final class JOperatorTypes {
    private JOperatorTypes() {
    }

    public static Optional<JPrimitiveType> asPrimitive(JType type, JTypeSystem system) {
        if (type instanceof JPrimitiveType pt) {
            return Optional.of(pt);
        } else if (type instanceof JNoneType) {
            return Optional.empty();
        }
        // JLS §5.1.8 unboxing conversion
        return Optional.ofNullable(JTypeRelations.unboxedType(type));
    }

    public static boolean isBoolean(JPrimitiveType type, JTypeSystem system) {
        return type.typeEquals(system.constants().booleanType());
    }

    public static boolean isIntegral(JPrimitiveType type, JTypeSystem system) {
        JTypeConstants c = system.constants();
        return type.typeEquals(c.byteType()) || type.typeEquals(c.shortType()) || type.typeEquals(c.charType()) ||
                type.typeEquals(c.intType()) || type.typeEquals(c.longType());
    }

    public static boolean isNumeric(JPrimitiveType type, JTypeSystem system) {
        return !isBoolean(type, system);
    }

    /**
     * JLS §5.6.1 unary numeric promotion: byte/short/char widen to int, everything else numeric is
     * unchanged. Returns empty for boolean or non-numeric-representable types.
     */
    public static Optional<JPrimitiveType> unaryNumericPromotion(JType type, JTypeSystem system) {
        return asPrimitive(type, system).flatMap(pt -> {
            JTypeConstants c = system.constants();
            if (isBoolean(pt, system)) {
                return Optional.empty();
            } else if (pt.typeEquals(c.byteType()) || pt.typeEquals(c.shortType()) || pt.typeEquals(c.charType())) {
                return Optional.of(c.intType());
            } else {
                return Optional.of(pt);
            }
        });
    }

    /**
     * JLS §5.6.2 binary numeric promotion: widest of {double, float, long, int} that either operand
     * requires, after unboxing. Returns empty if either side isn't numeric.
     */
    public static Optional<JPrimitiveType> binaryNumericPromotion(JType left, JType right, JTypeSystem system) {
        Optional<JPrimitiveType> lp = asPrimitive(left, system);
        Optional<JPrimitiveType> rp = asPrimitive(right, system);
        if (lp.isEmpty() || rp.isEmpty()) {
            return Optional.empty();
        }

        JTypeConstants c = system.constants();
        JPrimitiveType l = lp.get();
        JPrimitiveType r = rp.get();
        if (isBoolean(l, system) || isBoolean(r, system)) {
            return Optional.empty();
        } else if (l.typeEquals(c.doubleType()) || r.typeEquals(c.doubleType())) {
            return Optional.of(c.doubleType());
        } else if (l.typeEquals(c.floatType()) || r.typeEquals(c.floatType())) {
            return Optional.of(c.floatType());
        } else if (l.typeEquals(c.longType()) || r.typeEquals(c.longType())) {
            return Optional.of(c.longType());
        } else {
            return Optional.of(c.intType());
        }
    }

    /**
     * As {@link #binaryNumericPromotion} but rejects float/double, for the bitwise operators
     * ({@code & ^ |} in their integral form) and shift operators, which are only defined over the
     * integral primitive types.
     */
    public static Optional<JPrimitiveType> binaryIntegralPromotion(JType left, JType right, JTypeSystem system) {
        return binaryNumericPromotion(left, right, system)
                .filter(pt -> !pt.typeEquals(system.constants().floatType()) && !pt.typeEquals(system.constants().doubleType()));
    }
}
