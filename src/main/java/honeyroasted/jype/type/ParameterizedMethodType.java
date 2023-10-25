package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;

import java.util.HashSet;
import java.util.Set;

public interface ParameterizedMethodType extends PossiblyUnmodifiable, MethodType, ParameterizedType {

    ClassType outerType();

    void setOuterType(ClassType outerType);

    void setMethodReference(MethodReference methodReference);

    @Override
    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        return this.typeArguments().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

    default boolean hasTypeArguments() {
        return !this.typeArguments().isEmpty();
    }

}
