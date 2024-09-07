package honeyroasted.jype.type;

import honeyroasted.jype.system.visitor.JTypeVisitor;

public interface JNoneType extends JType {
    String name();

    default <R, P> R accept(JTypeVisitor<R, P> visitor, P context) {
        return visitor.visitNoneType(this, context);
    }

    @Override
    default boolean isNullType() {
        return this.name().equals("null");
    }
}
