package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JFieldReference;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

public interface JSimpleTypeVisitor<R, P> extends JTypeVisitor<R, P> {

    @Override
    default R visitClassType(JClassType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitPrimitiveType(JPrimitiveType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitWildcardType(JWildType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitArrayType(JArrayType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitIntersectionType(JIntersectionType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitMethodType(JMethodType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitVarType(JVarType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitMetaVarType(JMetaVarType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitNoneType(JNoneType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitFieldType(JFieldReference type, P context) {
        return visitType(type, context);
    }
}
