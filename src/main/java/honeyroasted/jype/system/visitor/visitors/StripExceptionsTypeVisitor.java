package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.MethodType;
import honeyroasted.jype.type.Type;

import java.util.Collections;

public class StripExceptionsTypeVisitor implements DeepStructuralMappingVisitor {

    @Override
    public boolean overridesMethodType(MethodType type) {
        return type instanceof MethodReference && !type.exceptionTypes().isEmpty();
    }

    @Override
    public Type methodTypeOverride(MethodType type, TypeCache<Type, Type> cache) {
        if (type instanceof MethodReference mr) {
            MethodReference copy = mr.copy(cache);
            copy.setUnmodifiable(false);
            copy.setExceptionTypes(Collections.emptyList());
            copy.setUnmodifiable(true);
            return copy;
        }
        return this.visit(type, cache);
    }
}
