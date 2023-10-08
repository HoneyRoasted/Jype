package honeyroasted.jype.type;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.system.visitor.TypeVisitor;

public interface PrimitiveType extends Type {
    ClassNamespace namespace();

    ClassReference box();

    String name();

    String descriptor();

    @Override
    default PrimitiveType stripMetadata() {
        return this;
    }

    @Override
    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitPrimitiveType(this, context);
    }
}
