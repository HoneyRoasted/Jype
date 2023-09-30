package honeyroasted.jype.system.visitor;

import honeyroasted.jype.system.solver.MetaKind;
import honeyroasted.jype.system.solver.TypeWithMetadata;
import honeyroasted.jype.type.*;

public interface TypeVisitor<R, P> {

    default R visit(Type type, P context) {
        return type.accept(this, context);
    }

    default R visitType(Type type, P context) {
        throw new UnsupportedOperationException();
    }

    default <T extends Type> R visitMetadataType(TypeWithMetadata<T> type, P context) {
        if (type.metadata().has(MetaKind.ERROR)) {
            return visitErrorType(type, context);
        } else if (type.metadata().has(MetaKind.UNDET_VAR)) {
            return visitUndetVar((TypeWithMetadata<VarType>) type, context);
        } else if (type.metadata().has(MetaKind.CAPTURED)) {
            return visitCapturedType((TypeWithMetadata<WildType>) type, context);
        }
        return type.type().accept(this, context);
    }

    R visitClassType(ClassType type, P context);

    R visitPrimitiveType(PrimitiveType type, P context);

    R visitWildcardType(WildType type, P context);

    R visitArrayType(ArrayType type, P context);

    R visitMethodType(MethodType type, P context);

    R visitTypeVar(VarType type, P context);

    R visitCapturedType(TypeWithMetadata<WildType> type, P context);

    R visitUndetVar(TypeWithMetadata<VarType> type, P context);

    R visitErrorType(TypeWithMetadata<? extends Type> errorType, P context);

}
