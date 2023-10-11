package honeyroasted.jype.system.visitor;

import honeyroasted.jype.system.visitor.visitors.SimpleTypeVisitor;
import honeyroasted.jype.type.ArrayType;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.MethodType;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;
import honeyroasted.jype.type.WildType;

import java.util.function.Function;
import java.util.function.Supplier;

public interface TypeVisitor<R, P> {

    default TypeVisitor<R, Void> withContext(P newContext) {
        return (TypeVisitor.Default<R, Void>) (type, context) -> this.visit(type, newContext);
    }

    default TypeVisitor<R, Void> withContext(Supplier<P> newContext) {
        return (TypeVisitor.Default<R, Void>) (type, context) -> this.visit(type, newContext.get());
    }

    default <T> TypeVisitor<T, P> mapResult(Function<R, T> mapper) {
        return (TypeVisitor.Default<T, P>) (type, context) -> mapper.apply(visit(type, context));
    }

    interface Default<R, P> extends SimpleTypeVisitor<R, P> {
        @Override
        R visitType(Type type, P context);
    }

    default R visit(Type type, P context) {
        if (type == null) return null;
        return type.accept(this, context);
    }

    default R visit(Type type) {
        return this.visit(type, null);
    }

    default R visitType(Type type, P context) {
        return null;
    }

    R visitClassType(ClassType type, P context);

    R visitPrimitiveType(PrimitiveType type, P context);

    R visitWildcardType(WildType type, P context);

    R visitArrayType(ArrayType type, P context);

    R visitMethodType(MethodType type, P context);

    R visitVarType(VarType type, P context);

    R visitMetaVarType(MetaVarType type, P context);

    R visitNoneType(NoneType type, P context);
}
