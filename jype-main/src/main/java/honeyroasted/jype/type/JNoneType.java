package honeyroasted.jype.type;

import honeyroasted.jype.metadata.location.JClassNamespace;
import honeyroasted.jype.system.visitor.JTypeVisitor;

import java.util.Optional;

public interface JNoneType extends JType, JReferencableType {

    JClassNamespace namespace();

    @Override
    default Optional<JClassNamespace> classNamespace() {
        return Optional.of(this.namespace());
    }

    String name();

    default <R, P> R accept(JTypeVisitor<R, P> visitor, P context) {
        return visitor.visitNoneType(this, context);
    }

    @Override
    default boolean isNullType() {
        return this.name().equals("null");
    }
}
