package honeyroasted.jype.system.visitor;

import honeyroasted.jype.system.visitor.visitors.SimpleTypeVisitor;
import honeyroasted.jype.type.*;

public interface TypeVisitor<R, P> {

    default TypeVisitor<R, Void> withContext(P newContext) {
        return new SimpleTypeVisitor<>() {
            @Override
            public R visitClassType(ClassType type, Void context) {
                return TypeVisitor.this.visit(type, newContext);
            }
        };
    }

    default R visit(Type type, P context) {
        if (type == null) return null;
        return type.accept(this, context);
    }

    default R visitType(Type type, P context) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    R visitClassType(ClassType type, P context);

    R visitPrimitiveType(PrimitiveType type, P context);

    R visitWildcardType(WildType type, P context);

    R visitArrayType(ArrayType type, P context);

    R visitMethodType(MethodType type, P context);

    R visitVarType(VarType type, P context);

    R visitNoneType(NoneType type, P context);
}
