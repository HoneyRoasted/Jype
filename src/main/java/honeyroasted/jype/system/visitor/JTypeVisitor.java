package honeyroasted.jype.system.visitor;

import honeyroasted.jype.system.visitor.visitors.JSimpleTypeVisitor;
import honeyroasted.jype.type.JArrayType;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JIntersectionType;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JMethodType;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;
import honeyroasted.jype.type.JWildType;

import java.util.function.Function;
import java.util.function.Supplier;

public interface JTypeVisitor<R, P> {

    default JTypeVisitor<R, Void> withContext(P newContext) {
        return (JTypeVisitor.Default<R, Void>) (type, context) -> this.visit(type, newContext);
    }

    default JTypeVisitor<R, Void> withContext(Supplier<P> newContext) {
        return (JTypeVisitor.Default<R, Void>) (type, context) -> this.visit(type, newContext.get());
    }

    default <T> JTypeVisitor<T, P> mapResult(Function<R, T> mapper) {
        return (JTypeVisitor.Default<T, P>) (type, context) -> mapper.apply(visit(type, context));
    }

    interface Default<R, P> extends JSimpleTypeVisitor<R, P> {
        @Override
        R visitType(JType type, P context);
    }

    default R visit(JType type, P context) {
        if (type == null) return null;
        return type.accept(this, context);
    }

    default R visit(JType type) {
        return this.visit(type, null);
    }

    default R visitType(JType type, P context) {
        return null;
    }

    R visitClassType(JClassType type, P context);

    R visitPrimitiveType(JPrimitiveType type, P context);

    R visitWildcardType(JWildType type, P context);

    R visitArrayType(JArrayType type, P context);

    R visitIntersectionType(JIntersectionType type, P context);

    R visitMethodType(JMethodType type, P context);

    R visitVarType(JVarType type, P context);

    R visitMetaVarType(JMetaVarType type, P context);

    R visitNoneType(JNoneType type, P context);
}
