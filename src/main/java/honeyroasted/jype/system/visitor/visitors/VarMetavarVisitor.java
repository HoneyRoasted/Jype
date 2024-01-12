package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.impl.MetaVarTypeImpl;

import java.util.Optional;

public class VarMetavarVisitor implements DeepStructuralTypeMappingVisitor {

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

        MetaVarType mvt = new MetaVarTypeImpl(type.typeSystem(), System.identityHashCode(type), type.simpleName());
        cache.put(type, mvt);
        type.upperBounds().forEach(bound -> mvt.upperBounds().add(this.visitType(bound, cache)));

        return DeepStructuralTypeMappingVisitor.super.varTypeOverride(type, cache);
    }
}
