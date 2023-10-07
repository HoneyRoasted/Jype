package honeyroasted.jype.type;

import honeyroasted.jype.system.visitor.TypeVisitor;

public interface NoneType extends Type {
    String name();

    @Override
    default NoneType stripMetadata() {
        return this;
    }

    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitNoneType(this, context);
    }
}
