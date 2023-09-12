package honeyroasted.jype.system.visitor;

import honeyroasted.jype.type.Type;

import java.util.List;
import java.util.function.Function;

public class TypeMapping<P> extends MapVisitor<P> implements Function<Type, Type> {
    @Override
    public Type apply(Type type) {
        return visit(type);
    }

    public List<Type> visit(List<Type> types, P context) {
        return types.stream().map(t -> visit(t, context)).toList();
    }

}
