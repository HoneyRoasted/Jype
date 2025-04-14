package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class JVarTypeResolveVisitor implements JDeepStructuralTypeMappingVisitor {
    private Predicate<JVarType> resolves;
    private Function<JVarType, JType> resolver;

    public JVarTypeResolveVisitor(Predicate<JVarType> resolves, Function<JVarType, JType> resolver) {
        this.resolves = resolves;
        this.resolver = resolver;
    }

    public JVarTypeResolveVisitor(Map<JVarType, ? extends JType> varMap) {
        this(varMap::containsKey, varMap::get);
    }

    @Override
    public boolean visitStructural() {
        return false;
    }

    @Override
    public boolean overridesVarType(JVarType type) {
        return this.resolves.test(type);
    }

    @Override
    public JType varTypeOverride(JVarType type, JTypeCache<JType, JType> cache) {
        JType resolved = this.resolver.apply(type);
        cache.put(type, resolved);
        if (resolved.typeEquals(type)) {
            return resolved;
        } else {
            return visit(resolved);
        }
    }

    public JVarTypeResolveVisitor and(JVarTypeResolveVisitor other) {
        return new JVarTypeResolveVisitor(this.resolves.or(other.resolves), t -> {
            if (this.resolves.test(t)) {
                return this.resolver.apply(t);
            }
            return other.resolver.apply(t);
        });
    }
}
