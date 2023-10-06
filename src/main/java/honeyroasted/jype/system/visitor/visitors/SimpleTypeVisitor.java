package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.solver.TypeWithMetadata;
import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.type.*;

public interface SimpleTypeVisitor<R, P> extends TypeVisitor<R, P> {

    @Override
    R visitType(Type type, P context);

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
    default R visitMethodType(MethodType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitVarType(VarType type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitCapturedType(TypeWithMetadata<WildType> type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitUndetVar(TypeWithMetadata<VarType> type, P context) {
        return visitType(type, context);
    }

    @Override
    default R visitErrorType(TypeWithMetadata<? extends Type> type, P context) {
        return visitType(type, context);
    }

}
