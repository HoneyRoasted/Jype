package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class JMetaVarTypeResolveVisitor implements JDeepStructuralTypeMappingVisitor {
    private Predicate<JMetaVarType> resolves;
    private Function<JMetaVarType, JType> resolver;

    public JMetaVarTypeResolveVisitor(Predicate<JMetaVarType> resolves, Function<JMetaVarType, JType> resolver) {
        this.resolves = resolves;
        this.resolver = resolver;
    }

    public JMetaVarTypeResolveVisitor(Map<JMetaVarType, ? extends JType> varMap) {
        this(varMap::containsKey, varMap::get);
    }

    @Override
    public boolean visitStructural() {
        return false;
    }

    @Override
    public boolean overridesMetaVarType(JMetaVarType type) {
        return this.resolves.test(type);
    }

    @Override
    public JType metaVarTypeOverride(JMetaVarType type, JTypeCache<JType, JType> cache) {
        JType resolved = this.resolver.apply(type);
        cache.put(type, resolved);
        if (resolved.typeEquals(type)) {
            return resolved;
        } else {
            return visit(resolved);
        }
    }
}
