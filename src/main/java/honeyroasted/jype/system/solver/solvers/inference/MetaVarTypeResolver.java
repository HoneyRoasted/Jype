package honeyroasted.jype.system.solver.solvers.inference;

import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.visitor.visitors.DeepStructuralMappingVisitor;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class MetaVarTypeResolver implements DeepStructuralMappingVisitor {
    private Predicate<MetaVarType> resolves;
    private Function<MetaVarType, Type> resolver;

    public MetaVarTypeResolver(Predicate<MetaVarType> resolves, Function<MetaVarType, Type> resolver) {
        this.resolves = resolves;
        this.resolver = resolver;
    }

    public MetaVarTypeResolver(Map<MetaVarType, ? extends Type> varMap) {
        this(varMap::containsKey, varMap::get);
    }

    @Override
    public boolean overridesMetaVarType(MetaVarType type) {
        return this.resolves.test(type);
    }

    @Override
    public Type metaVarTypeOverride(MetaVarType type, TypeCache<Type, Type> cache) {
        Type resolved = this.resolver.apply(type);
        cache.put(type, resolved);
        if (resolved.typeEquals(type)) {
            return resolved;
        } else {
            return visit(resolved);
        }
    }
}
