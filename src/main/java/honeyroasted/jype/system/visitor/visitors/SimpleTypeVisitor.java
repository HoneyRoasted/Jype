package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.solver.TypeWithMetadata;
import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.type.*;

public abstract class SimpleTypeVisitor<R, P> implements TypeVisitor<R, P> {

    @Override
    public abstract R visitType(Type type, P context);

    @Override
    public R visitClassType(ClassType type, P context) {
        return visitType(type, context);
    }

    @Override
    public R visitPrimitiveType(PrimitiveType type, P context) {
        return visitType(type, context);
    }

    @Override
    public R visitWildcardType(WildType type, P context) {
        return visitType(type, context);
    }

    @Override
    public R visitArrayType(ArrayType type, P context) {
        return visitType(type, context);
    }

    @Override
    public R visitMethodType(MethodType type, P context) {
        return visitType(type, context);
    }

    @Override
    public R visitTypeVar(VarType type, P context) {
        return visitType(type, context);
    }

    @Override
    public R visitCapturedType(TypeWithMetadata<WildType> type, P context) {
        return visitType(type, context);
    }

    @Override
    public R visitUndetVar(TypeWithMetadata<VarType> type, P context) {
        return visitType(type, context);
    }

    @Override
    public R visitErrorType(TypeWithMetadata<? extends Type> type, P context) {
        return visitType(type, context);
    }

}
