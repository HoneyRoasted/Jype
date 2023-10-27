package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;
import honeyroasted.jype.type.impl.WildTypeUpperImpl;

import java.util.Optional;

public class VarWildcardingVisitor implements DeepStructuralTypeMappingVisitor {

    @Override
    public boolean visitStructural() {
        return false;
    }

    @Override
    public boolean overridesVarType(VarType type) {
        return true;
    }

    @Override
    public Type varTypeOverride(VarType type, TypeCache<Type, Type> cache) {
        Optional<Type> cached = cache.get(type);
        if (cached.isPresent()) return cached.get();

        WildType.Upper wtu = new WildTypeUpperImpl(type.typeSystem());
        wtu.setIdentity(System.identityHashCode(type));
        cache.put(type, wtu);
        type.upperBounds().forEach(bound -> wtu.upperBounds().add(this.visit(bound, cache)));

        return wtu;
    }
}
