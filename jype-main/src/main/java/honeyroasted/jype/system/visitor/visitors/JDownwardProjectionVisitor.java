package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class JDownwardProjectionVisitor implements JStructuralTypeMappingVisitor<Set<JType>> {
    private final Predicate<JType> restricted;

    public JDownwardProjectionVisitor(Predicate<JType> restricted) {
        this.restricted = restricted;
    }

    private static Set<JType> seen(Set<JType> seen) {
        return seen == null ? new HashSet<>() : seen;
    }

    @Override
    public JType visitVarType(JVarType type, Set<JType> context) {
        if (!this.restricted.test(type)) {
            return type;
        }
        if (!type.hasLowerBound()) {
            // JLS §4.10.5 a restricted type variable without a lower bound has no downward projection
            throw new UndefinedProjectionException(type + " has no lower bounds");
        }
        return this.projectBound(type, type.lowerBound(), seen(context));
    }

    @Override
    public JType visitMetaVarType(JMetaVarType type, Set<JType> context) {
        if (!this.restricted.test(type)) {
            return type;
        }
        if (type.lowerBounds().isEmpty()) {
            throw new UndefinedProjectionException(type + " has no lower bounds");
        }
        return this.projectBound(type, type.lowerBound(), seen(context));
    }

    private JType projectBound(JType variable, JType bound, Set<JType> seen) {
        if (!seen.add(variable)) {
            throw new UndefinedProjectionException(variable + " has a cyclic lower bound");
        }
        try {
            // JLS §4.10.5 a restricted type variable projects to the downward projection of its lower bound
            return this.visit(bound, seen);
        } finally {
            seen.remove(variable);
        }
    }

    @Override
    public JType visitClassType(JClassType type, Set<JType> context) {
        if (!(type instanceof JParameterizedClassType pct) || !type.mentions(this.restricted)) {
            return type;
        }
        Set<JType> seen = seen(context);
        JParameterizedClassType result = type.typeSystem().typeFactory().newParameterizedClassType();
        result.setClassReference(pct.classReference());
        if (pct.outerType() != null) {
            result.setOuterType((JClassType) this.visit(pct.outerType(), seen));
        }
        List<JArgumentType> args = new ArrayList<>();
        for (JArgumentType arg : pct.typeArguments()) {
            if (!arg.mentions(this.restricted)) {
                args.add(arg);
            } else if (arg instanceof JWildType.Upper upper) {
                // JLS §4.10.5 ? extends U projects to ? extends U' when U has a downward projection
                JWildType.Upper wildcard = type.typeSystem().typeFactory().newUpperWildType();
                JType projected = this.visit(upper.upperBound(), seen);
                wildcard.setUpperBounds(projected instanceof JIntersectionType it ? it.children() : Set.of(projected));
                wildcard.setUnmodifiable(true);
                args.add(wildcard);
            } else if (arg instanceof JWildType.Lower lower) {
                // JLS §4.10.5 ? super L projects to ? super L' with L' the upward projection of L
                JWildType.Lower wildcard = type.typeSystem().typeFactory().newLowerWildType();
                wildcard.setLowerBounds(Set.of(new JUpwardProjectionVisitor(this.restricted).visit(lower.lowerBound(), seen)));
                wildcard.setUnmodifiable(true);
                args.add(wildcard);
            } else {
                // JLS §4.10.5 a non-wildcard argument mentioning a restricted variable has no downward projection
                throw new UndefinedProjectionException(arg + " is a non-wildcard type that mentions restricted variables");
            }
        }
        result.setTypeArguments(args);
        result.setUnmodifiable(true);
        return result;
    }

    public static class UndefinedProjectionException extends RuntimeException {
        public UndefinedProjectionException(String message) {
            super(message);
        }
    }
}
