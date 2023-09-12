package honeyroasted.jype.system.visitor;

import honeyroasted.jype.type.Type;

public class MapVisitor<P> extends DefaultTypeVisitor<Type, P> {

    public final Type visit(Type type) {
        return this.visit(type, null);
    }

    @Override
    public Type visitType(Type type, P context) {
        return type;
    }
}
