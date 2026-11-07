package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.solver.JTypeRelations;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class JUpwardProjectionVisitor implements JStructuralTypeMappingVisitor<Set<JType>> {
    private final Predicate<JType> restricted;

    public JUpwardProjectionVisitor(Predicate<JType> restricted) {
        this.restricted = restricted;
    }

    private boolean mentions(JType type) {
        return type.mentions(this.restricted);
    }

    private static Set<JType> seen(Set<JType> seen) {
        return seen == null ? new HashSet<>() : seen;
    }

    @Override
    public JType visitVarType(JVarType type, Set<JType> context) {
        if (!this.restricted.test(type)) {
            return type;
        }
        return this.projectBound(type, type.upperBound(), seen(context));
    }

    @Override
    public JType visitMetaVarType(JMetaVarType type, Set<JType> context) {
        if (!this.restricted.test(type)) {
            return type;
        }
        return this.projectBound(type, type.upperBound(), seen(context));
    }

    private JType projectBound(JType variable, JType bound, Set<JType> seen) {
        if (!seen.add(variable)) {
            return variable.typeSystem().constants().object();
        }
        try {
            // JLS §4.10.5 a restricted type variable projects to the upward projection of its upper bound
            return this.visit(bound, seen);
        } finally {
            seen.remove(variable);
        }
    }

    @Override
    public JType visitClassType(JClassType type, Set<JType> context) {
        if (!(type instanceof JParameterizedClassType pct) || !this.mentions(type)) {
            return type;
        }
        Set<JType> seen = seen(context);
        JParameterizedClassType result = type.typeSystem().typeFactory().newParameterizedClassType();
        result.setClassReference(pct.classReference());
        if (pct.outerType() != null) {
            result.setOuterType((JClassType) this.visit(pct.outerType(), seen));
        }
        List<JArgumentType> args = new ArrayList<>();
        List<JVarType> params = pct.typeParameters();
        for (int i = 0; i < pct.typeArguments().size(); i++) {
            args.add(this.projectArgument(pct, pct.typeArguments().get(i), i < params.size() ? params.get(i) : null, seen));
        }
        result.setTypeArguments(args);
        result.setUnmodifiable(true);
        return result;
    }

    private JArgumentType projectArgument(JParameterizedClassType pct, JArgumentType arg, JVarType param, Set<JType> seen) {
        if (!this.mentions(arg)) {
            return arg;
        }
        if (arg instanceof JWildType.Upper upper) {
            // JLS §4.10.5 ? extends U projects to ? extends U'
            return upper.upperBounds().isEmpty() ? arg : extendsWildcard(this.visit(upper.upperBound(), seen));
        } else if (arg instanceof JWildType.Lower lower) {
            // JLS §4.10.5 ? super L projects to ? super L' when L has a downward projection, otherwise ?
            return this.downward(lower.lowerBound(), seen).map(JUpwardProjectionVisitor::superWildcard).orElseGet(() -> unbounded(arg));
        }
        JType u = this.visit(arg, seen);
        JType object = arg.typeSystem().constants().object();
        boolean boundMentionsParams = param != null && param.upperBounds().stream().anyMatch(b -> b.mentions(t -> t instanceof JVarType v && pct.typeParameters().contains(v)));
        if (!u.typeEquals(object) && (boundMentionsParams || param == null || !JTypeRelations.isSubtype(param.upperBound(), u))) {
            // JLS §4.10.5 ? extends U when U is not Object and the declared bound does not already imply it
            return extendsWildcard(u);
        }
        // JLS §4.10.5 otherwise ? super L when the downward projection L exists, else ?
        return this.downward(arg, seen).map(JUpwardProjectionVisitor::superWildcard).orElseGet(() -> unbounded(arg));
    }

    private Optional<JType> downward(JType type, Set<JType> seen) {
        try {
            return Optional.of(new JDownwardProjectionVisitor(this.restricted).visit(type, seen));
        } catch (JDownwardProjectionVisitor.UndefinedProjectionException ex) {
            return Optional.empty();
        }
    }

    private static JArgumentType extendsWildcard(JType bound) {
        if (bound.typeEquals(bound.typeSystem().constants().object())) {
            return unbounded(bound);
        }
        JWildType.Upper wildcard = bound.typeSystem().typeFactory().newUpperWildType();
        wildcard.setUpperBounds(bound instanceof JIntersectionType it ? it.children() : Set.of(bound));
        wildcard.setUnmodifiable(true);
        return wildcard;
    }

    private static JArgumentType superWildcard(JType bound) {
        JWildType.Lower wildcard = bound.typeSystem().typeFactory().newLowerWildType();
        wildcard.setLowerBounds(Set.of(bound));
        wildcard.setUnmodifiable(true);
        return wildcard;
    }

    private static JArgumentType unbounded(JType any) {
        JWildType.Upper wildcard = any.typeSystem().typeFactory().newUpperWildType();
        wildcard.setUnmodifiable(true);
        return wildcard;
    }
}
