package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class JVarMetavarVisitor implements JDeepStructuralTypeMappingVisitor {
    private Predicate<JVarType> include;
    private Predicate<JVarType> exclude;

    public JVarMetavarVisitor(Predicate<JVarType> include, Predicate<JVarType> exclude) {
        this.include = include;
        this.exclude = exclude;
    }

    public JVarMetavarVisitor() {
        this(null, null);
    }

    public static JVarMetavarVisitor including(Predicate<JVarType> include) {
        return new JVarMetavarVisitor(include, null);
    }

    public static JVarMetavarVisitor including(Set<JVarType> include) {
        return including(include::contains);
    }

    public static JVarMetavarVisitor excluding(Predicate<JVarType> exclude) {
        return new JVarMetavarVisitor(null, exclude);
    }

    public static JVarMetavarVisitor excluding(Set<JVarType> exclude) {
        return excluding(exclude::contains);
    }

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

        if ((this.include != null && !this.include.test(type)) || (this.exclude != null && this.exclude.test(type))) {
            return type;
        }

        JMetaVarType mvt = type.typeSystem().typeFactory().newMetaVarType(System.identityHashCode(type), type.simpleName());
        cache.put(type, mvt);
        type.upperBounds().forEach(bound -> mvt.upperBounds().add(this.visitType(bound, cache)));

        return mvt;
    }
}
