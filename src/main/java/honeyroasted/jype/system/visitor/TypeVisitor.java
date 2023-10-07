package honeyroasted.jype.system.visitor;

import honeyroasted.jype.type.*;

public interface TypeVisitor<R, P> {

    default R visit(Type type, P context) {
        if (type == null) return null;
        return type.accept(this, context);
    }

    default R visitType(Type type, P context) {
        throw new UnsupportedOperationException();
    }

    R visitClassType(ClassType type, P context);

    R visitPrimitiveType(PrimitiveType type, P context);

    R visitWildcardType(WildType type, P context);

    R visitArrayType(ArrayType type, P context);

    R visitMethodType(MethodType type, P context);

    R visitVarType(VarType type, P context);

    R visitNoneType(NoneType type, P context);
}
