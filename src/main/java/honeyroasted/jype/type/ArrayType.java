package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.Set;

public interface ArrayType extends PossiblyUnmodifiable, InstantiableType, ArgumentType {
    Type component();

    void setComponent(Type component);

    int depth();

    @Override
    default Set<Type> knownDirectSupertypes() {
        return Set.of(typeSystem().constants().object());
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        return this.component().hasCyclicTypeVariables();
    }

    @Override
    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitArrayType(this, context);
    }
}
