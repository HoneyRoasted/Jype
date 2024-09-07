package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

public class JErasureTypeVisitor implements JStructuralTypeMappingVisitor<Boolean> {

    @Override
    public JType visit(JType type) {
        return this.visit(type, true);
    }

    @Override
    public JType visitWildcardType(JWildType type, Boolean recurse) {
        return visit(type.upperBounds().iterator().next(), recurse);
    }

    @Override
    public JType visitClassType(JClassType type, Boolean recurse) {
        if (type instanceof JParameterizedClassType pType) {
            JClassReference ref = pType.classReference();
            if (!recurse) {
                JParameterizedClassType ct = ref.parameterized();
                ct.setUnmodifiable(false);
                ct.setOuterType(pType.outerType());
                ct.setUnmodifiable(true);
                return ct;
            }
            return ref;
        }
        return type;
    }

    @Override
    public JType visitVarType(JVarType type, Boolean recurse) {
        return visit(type.upperBounds().isEmpty() ? type.typeSystem().constants().object() : type.upperBounds().iterator().next(), recurse);
    }
}
