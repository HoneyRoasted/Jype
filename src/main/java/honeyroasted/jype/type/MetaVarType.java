package honeyroasted.jype.type;

import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.Set;

public interface MetaVarType extends Type, ArgumentType {

    int identity();

    String name();

    Set<Type> upperBounds();

    Set<Type> lowerBounds();

    Set<Type> equalities();

    @Override
    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitMetaVarType(this, context);
    }
}
