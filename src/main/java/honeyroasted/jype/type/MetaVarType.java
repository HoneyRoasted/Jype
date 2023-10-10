package honeyroasted.jype.type;

import honeyroasted.jype.system.visitor.TypeVisitor;

public interface MetaVarType extends Type {

    int identity();

    String name();

    @Override
    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitMetaVarType(this, context);
    }
}
