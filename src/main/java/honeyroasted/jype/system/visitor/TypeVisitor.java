package honeyroasted.jype.system.visitor;

import honeyroasted.jype.type.*;

public interface TypeVisitor<R, P> {

    default R visit(Type type, P context) {
        return type.accept(this, context);
    }

    default R visitType(Type type, P context) {
        throw new UnsupportedOperationException();
    }

    R visitArray(ArrayType type, P context);

    R visitClassRef(ClassReference type, P context);

    R visitClass(ClassType type, P context);

    R visitMethodRef(MethodReference type, P context);

    R visitMethod(MethodType type, P context);

    R visitNone(NoneType type, P context);

    R visitPrimitive(PrimitiveType type, P context);

    R visitVariable(VarType type, P context);

    R visitWild(WildType type, P context);

}
