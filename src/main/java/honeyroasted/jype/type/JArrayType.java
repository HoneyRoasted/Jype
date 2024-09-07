package honeyroasted.jype.type;

import honeyroasted.collect.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.visitor.JTypeVisitor;

import java.util.Set;

public interface JArrayType extends PossiblyUnmodifiable, JInstantiableType, JArgumentType {
    JType component();

    default JType deepComponent() {
        return this.component() instanceof JArrayType at ? at.deepComponent() : this.component();
    }

    void setComponent(JType component);

    int depth();

    @Override
    default Set<JType> knownDirectSupertypes() {
        return Set.of(typeSystem().constants().object());
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<JVarType> seen) {
        return this.component().hasCyclicTypeVariables();
    }

    @Override
    default <R, P> R accept(JTypeVisitor<R, P> visitor, P context) {
        return visitor.visitArrayType(this, context);
    }
}
