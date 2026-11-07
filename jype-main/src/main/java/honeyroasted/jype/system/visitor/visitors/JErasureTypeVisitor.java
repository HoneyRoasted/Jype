package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.List;

public class JErasureTypeVisitor implements JStructuralTypeMappingVisitor<Boolean> {

    @Override
    public JType visit(JType type) {
        return this.visit(type, true);
    }

    @Override
    public JType visitWildcardType(JWildType type, Boolean recurse) {
        if (type instanceof JWildType.Upper upper && !upper.upperBounds().isEmpty()) {
            return visit(upper.upperBounds().iterator().next(), recurse);
        }
        return type.typeSystem().constants().object();
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
            // JLS §4.6 the erasure of a parameterized type is |G|
            return ref;
        }
        return type;
    }

    @Override
    public JType visitVarType(JVarType type, Boolean recurse) {
        // JLS §4.6 the erasure of a type variable is the erasure of its leftmost bound
        return visit(type.upperBounds().isEmpty() ? type.typeSystem().constants().object() : type.upperBounds().iterator().next(), recurse);
    }

    @Override
    public JType visitIntersectionType(JIntersectionType type, Boolean recurse) {
        // JLS §4.6 the erasure of an intersection type is the erasure of its leftmost component
        return visit(type.children().iterator().next(), recurse);
    }

    @Override
    public JType visitMethodType(JMethodType type, Boolean recurse) {
        JType mapped = JStructuralTypeMappingVisitor.super.visitMethodType(type, recurse);
        if (mapped instanceof JMethodReference ref && !ref.typeParameters().isEmpty()) {
            // JLS §4.6 an erased method signature has no type parameters
            JMethodReference erased = ref.typeSystem().typeFactory().newMethodReference();
            erased.setLocation(ref.location());
            erased.setModifiers(ref.modifiers());
            erased.setOuterClass(ref.outerClass());
            erased.setExceptionTypes(ref.exceptionTypes());
            erased.setTypeParameters(List.of());
            erased.setParameters(ref.parameters());
            erased.setReturnType(ref.returnType());
            erased.setUnmodifiable(true);
            return erased;
        }
        return mapped;
    }
}
