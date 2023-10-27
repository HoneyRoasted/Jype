package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class VarTypeResolveVisitor implements DeepStructuralTypeMappingVisitor {
    private Predicate<VarType> resolves;
    private Function<VarType, Type> resolver;

    public VarTypeResolveVisitor(Predicate<VarType> resolves, Function<VarType, Type> resolver) {
        this.resolves = resolves;
        this.resolver = resolver;
    }

    public VarTypeResolveVisitor(Map<VarType, ? extends Type> varMap) {
        this(varMap::containsKey, varMap::get);
    }

    @Override
    public boolean visitStructural() {
        return false;
    }

    @Override
    public boolean overridesVarType(VarType type) {
        return this.resolves.test(type);
    }

    @Override
    public Type varTypeOverride(VarType type, TypeCache<Type, Type> cache) {
        Type resolved = this.resolver.apply(type);
        cache.put(type, resolved);
        if (resolved.typeEquals(type)) {
            return resolved;
        } else {
            return visit(resolved);
        }
    }

    public VarTypeResolveVisitor and(VarTypeResolveVisitor other) {
        return new VarTypeResolveVisitor(this.resolves.or(other.resolves), t -> {
            if (this.resolves.test(t)) {
                return this.resolver.apply(t);
            }
            return other.resolver.apply(t);
        });
    }
}
