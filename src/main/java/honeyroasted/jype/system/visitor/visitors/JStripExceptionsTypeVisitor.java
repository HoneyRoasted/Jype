package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JType;

import java.util.Collections;

public class JStripExceptionsTypeVisitor implements JDeepStructuralTypeMappingVisitor {

    @Override
    public boolean overridesMethodType(JMethodType type) {
        return type instanceof JMethodReference && !type.exceptionTypes().isEmpty();
    }

    @Override
    public JType methodTypeOverride(JMethodType type, JTypeCache<JType, JType> cache) {
        if (type instanceof JMethodReference mr) {
            JMethodReference copy = mr.copy(cache);
            copy.setUnmodifiable(false);
            copy.setExceptionTypes(Collections.emptyList());
            copy.setUnmodifiable(true);
            return copy;
        }
        return this.visit(type, cache);
    }
}
