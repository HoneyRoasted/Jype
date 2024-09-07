package honeyroasted.jype.system.visitor.visitors;

import honeyroasted.jype.system.visitor.JTypeVisitor;
import honeyroasted.jype.type.JType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface JTypeMappingVisitor<P> extends JSimpleTypeVisitor<JType, P>, Function<JType, JType> {

    interface Default<P> extends JTypeMappingVisitor<P> {
        @Override
        JType visitType(JType type, P context);
    }

    default JTypeMappingVisitor<Void> withContext(Supplier<P> newContext) {
        return (JTypeMappingVisitor.Default<Void>) (type, context) -> this.visit(type, newContext.get());
    }

    default JType visit(JType type) {
        return type.accept(this, null);
    }

    @Override
    default JType apply(JType type) {
        return visit(type);
    }

    default List<JType> visit(List<? extends JType> types, P context) {
        return types.stream().map(t -> visit(t, context)).toList();
    }

    default Set<JType> visit(Set<JType> types, P context) {
        return types.stream().map(t -> visit(t, context)).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    default JType visitType(JType type, P context) {
        return type;
    }

    default <K, R> JTypeVisitor<R, K> andThen(JTypeVisitor<R, K> visitor, P newContext) {
        return (JTypeVisitor.Default<R, K>) (type, context) -> visitor.visit(this.visit(type, newContext), context);
    }

    default <K, R> JTypeVisitor<R, K> andThen(JTypeVisitor<R, K> visitor) {
        return (JTypeVisitor.Default<R, K>) (type, context) -> visitor.visit(this.visit(type), context);
    }
}