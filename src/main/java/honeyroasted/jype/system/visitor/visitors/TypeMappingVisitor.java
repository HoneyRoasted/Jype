package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.visitor.TypeVisitor;
import honeyroasted.jype.type.Type;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface TypeMappingVisitor<P> extends SimpleTypeVisitor<Type, P>, Function<Type, Type> {

    interface Default<P> extends TypeMappingVisitor<P> {
        @Override
        Type visitType(Type type, P context);
    }

    default TypeMappingVisitor<Void> withContext(Supplier<P> newContext) {
        return (TypeMappingVisitor.Default<Void>) (type, context) -> this.visit(type, newContext.get());
    }

    default Type visit(Type type) {
        return type.accept(this, null);
    }

    @Override
    default Type apply(Type type) {
        return visit(type);
    }

    default List<Type> visit(List<? extends Type> types, P context) {
        return types.stream().map(t -> visit(t, context)).toList();
    }

    default Set<Type> visit(Set<Type> types, P context) {
        return types.stream().map(t -> visit(t, context)).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    default Type visitType(Type type, P context) {
        return type;
    }

    default <K, R> TypeVisitor<R, K> andThen(TypeVisitor<R, K> visitor, P newContext) {
        return (TypeVisitor.Default<R, K>) (type, context) -> visitor.visit(this.visit(type, newContext), context);
    }

    default <K, R> TypeVisitor<R, K> andThen(TypeVisitor<R, K> visitor) {
        return (TypeVisitor.Default<R, K>) (type, context) -> visitor.visit(this.visit(type), context);
    }
}