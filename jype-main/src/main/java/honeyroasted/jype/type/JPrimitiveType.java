package honeyroasted.jype.type;

import honeyroasted.jype.metadata.location.JClassNamespace;
import honeyroasted.jype.metadata.signature.JDescriptor;
import honeyroasted.jype.system.visitor.JTypeVisitor;

import java.util.Optional;

public interface JPrimitiveType extends JReferencableType {
    JClassNamespace namespace();

    JClassNamespace boxNamespace();

    JClassReference box();

    @Override
    default Optional<JClassNamespace> classNamespace() {
        return Optional.of(this.namespace());
    }

    String name();

    JDescriptor.Primitive descriptor();

    @Override
    default <R, P> R accept(JTypeVisitor<R, P> visitor, P context) {
        return visitor.visitPrimitiveType(this, context);
    }
}
