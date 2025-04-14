package honeyroasted.jype.type;

import honeyroasted.collect.modify.PossiblyUnmodifiable;

import java.util.HashSet;
import java.util.Set;

public interface JParameterizedMethodType extends PossiblyUnmodifiable, JMethodType, JParameterizedType {

    JClassType outerType();

    void setOuterType(JClassType outerType);

    void setMethodReference(JMethodReference methodReference);

    @Override
    default boolean hasCyclicTypeVariables(Set<JVarType> seen) {
        return this.typeArguments().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

    default boolean hasTypeArguments() {
        return !this.typeArguments().isEmpty();
    }

}
