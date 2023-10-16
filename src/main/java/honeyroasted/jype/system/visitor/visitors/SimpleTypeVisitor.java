package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.MethodType;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

public interface SimpleTypeVisitor<R, P> extends TypeVisitor<R, P> {

    @Override
    default R visitClassType(ClassType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitPrimitiveType(PrimitiveType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitWildcardType(WildType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitArrayType(ArrayType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitIntersectionType(IntersectionType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitMethodType(MethodType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitVarType(VarType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitMetaVarType(MetaVarType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitNoneType(NoneType type, P context) {
        return visitType(type, context);
    }
}
