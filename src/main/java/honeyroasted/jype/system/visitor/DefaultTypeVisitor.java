package honeyroasted.jype.system.visitor;

import honeyroasted.jype.type.*;

public abstract class DefaultTypeVisitor<R, P> implements TypeVisitor<R, P> {
    @Override
    public R visitArray(ArrayType type, P context) {
        return visitType(type, context);
    }

    @Override
    public R visitClassRef(ClassReference type, P context) {
        return visitType(type, context);
    }

    @Override
    public R visitClass(ClassType type, P context) {
        return visitType(type, context);
    }

    @Override
    public R visitMethodRef(MethodReference type, P context) {
        return visitType(type, context);
    }

    @Override
    public R visitMethod(MethodType type, P context) {
        return visitType(type, context);
    }

    @Override
    public R visitNone(NoneType type, P context) {
        return visitType(type, context);
    }

    @Override
    public R visitPrimitive(PrimitiveType type, P context) {
        return visitType(type, context);
    }

    @Override
    public R visitVariable(VarType type, P context) {
        return visitType(type, context);
    }

    @Override
    public R visitWild(WildType type, P context) {
        return visitType(type, context);
    }
}
