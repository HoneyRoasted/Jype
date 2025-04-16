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

public class JUpwardProjectionVisitor implements JDeepStructuralTypeMappingVisitor {
    private Predicate<JType> restricted;

    public JUpwardProjectionVisitor(Predicate<JType> restricted) {
        this.restricted = restricted;
    }

    @Override
    public boolean visitStructural() {
        return false;
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
            } else {
                JType project = this.visit(ta, cache); //U
                JVarType vt = pct.typeParameters().get(i); //T extends Bi

                if (!project.equals(type.typeSystem().constants().object()) && //U != Object
                        (vt.upperBounds().stream().anyMatch(bound -> bound.accept(pct.typeParameters()::contains)) || //Bi mentions a typer parameter of G
                         !vt.upperBound().isAssignableTo(project))) { //or Bi is not a subtype of U
                    JWildType.Upper newArg = type.typeSystem().typeFactory().newUpperWildType();
                    newArg.setUpperBounds(project instanceof JIntersectionType inter ? inter.children() : Set.of(project));
                    newArg.setUnmodifiable(true);
                    result = newArg;
                } else {
                    JType downward = ta.accept(JTypeVisitors.downwardProjection(this.restricted));
                    if (downward != null) {
                        JWildType.Lower newArg = type.typeSystem().typeFactory().newLowerWildType();
                        newArg.setLowerBounds(downward instanceof JIntersectionType inter ? inter.children() : Set.of(downward));
                        newArg.setUnmodifiable(true);
                        result = newArg;
                    } else {
                        JWildType.Upper raw = type.typeSystem().typeFactory().newUpperWildType();
                        raw.setUnmodifiable(true);
                        result = raw;
                    }
                }
            }

            newType.typeArguments().add(result);
        }

        newType.setUnmodifiable(true);
        return newType;
    }

    @Override
    public boolean overridesVarType(JVarType type) {
        return this.restricted.test(type);
    }

    @Override
    public JType varTypeOverride(JVarType type, JTypeCache<JType, JType> cache) {
        return visit(type.upperBound(), cache);
    }

    @Override
    public boolean overridesMetaVarType(JMetaVarType type) {
        return this.restricted.test(type);
    }

    @Override
    public JType metaVarTypeOverride(JMetaVarType type, JTypeCache<JType, JType> cache) {
        return visit(type.upperBound(), cache);
    }

    @Override
    public boolean overridesClassType(JClassType type) {
        return type instanceof JParameterizedClassType && type.accept(this.restricted);
    }
}
