package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.system.visitor.JTypeVisitors;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class JDownwardProjectionVisitor implements JDeepStructuralTypeMappingVisitor {
    private Predicate<JType> restricted;

    public JDownwardProjectionVisitor(Predicate<JType> restricted) {
        this.restricted = restricted;
    }

    @Override
    public boolean visitStructural() {
        return false;
    }

    @Override
    public boolean overridesClassType(JClassType type) {
        return type instanceof JParameterizedClassType && type.accept(this.restricted);
    }

    @Override
    public JType classTypeOverride(JClassType type, JTypeCache<JType, JType> cache) {
        JParameterizedClassType pct = (JParameterizedClassType) type;
        JParameterizedClassType newType = type.typeSystem().typeFactory().newParameterizedClassType();
        cache.put(type, newType);

        List<JArgumentType> typeArguments = pct.typeArguments();
        for (int i = 0; i < typeArguments.size(); i++) {
            JArgumentType ta = typeArguments.get(i);
            JArgumentType result;
            if (!ta.accept(this.restricted)) {
                result = ta;
            } else if (ta instanceof JWildType.Lower lower) {
                JWildType.Lower newArg = type.typeSystem().typeFactory().newLowerWildType();
                JType projection = lower.lowerBound().accept(JTypeVisitors.upwardProjection(this.restricted));
                newArg.setLowerBounds(projection instanceof JIntersectionType it ? it.children() : Set.of(projection));
                newArg.setUnmodifiable(true);
                result = newArg;
            } else if (ta instanceof JWildType.Upper upper) {
                result = (JArgumentType) visit(upper, cache);
            } else {
                throw new UndefinedProjectionException(ta + " is a non-wildcard type that mentions restricted variables");
            }

            newType.typeArguments().add(result);
        }

        newType.setUnmodifiable(true);
        return newType;
    }

    @Override
    public boolean overridesVarType(JVarType type) {
        return type.accept(this.restricted);
    }

    @Override
    public JType varTypeOverride(JVarType type, JTypeCache<JType, JType> cache) {
        throw new UndefinedProjectionException(type + " has no lower bounds");
    }

    @Override
    public boolean overridesMetaVarType(JMetaVarType type) {
        return type.accept(this.restricted);
    }

    @Override
    public JType metaVarTypeOverride(JMetaVarType type, JTypeCache<JType, JType> cache) {
        if (type.lowerBounds().isEmpty()) {
            throw new UndefinedProjectionException(type + " has no lower bounds");
        }

        return visit(type.lowerBound(), cache);
    }

    public static class UndefinedProjectionException extends RuntimeException {
        public UndefinedProjectionException(String message) {
            super(message);
        }
    }

}
