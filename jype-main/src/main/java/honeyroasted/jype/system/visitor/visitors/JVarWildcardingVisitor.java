package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.Optional;

public class JVarWildcardingVisitor implements JDeepStructuralTypeMappingVisitor {

    @Override
    public boolean visitStructural() {
        return false;
    }

    @Override
    public boolean overridesVarType(JVarType type) {
        return true;
    }

    @Override
    public JType varTypeOverride(JVarType type, JTypeCache<JType, JType> cache) {
        Optional<JType> cached = cache.get(type);
        if (cached.isPresent()) return cached.get();

        JWildType.Upper wtu = type.typeSystem().typeFactory().newUpperWildType();
        wtu.setIdentity(System.identityHashCode(type));
        cache.put(type, wtu);
        type.upperBounds().forEach(bound -> wtu.upperBounds().add(this.visit(bound, cache)));

        return wtu;
    }
}
