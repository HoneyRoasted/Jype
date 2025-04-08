package honeyroasted.jype.type;

import honeyroasted.jype.metadata.location.JClassNamespace;
import honeyroasted.jype.system.visitor.JTypeVisitor;

public interface JPrimitiveType extends JType {
    JClassNamespace namespace();

    JClassNamespace boxNamespace();

    JClassReference box();

    String name();

    String descriptor();

    @Override
    default <R, P> R accept(JTypeVisitor<R, P> visitor, P context) {
        return visitor.visitPrimitiveType(this, context);
    }
}
