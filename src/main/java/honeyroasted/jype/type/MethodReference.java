package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface MethodReference extends PossiblyUnmodifiable, Type, MethodType {
    ParameterizedMethodType asMethodType(List<Type> typeArguments);

    ParameterizedMethodType asMethodType(Type... typeArguments);

    ParameterizedMethodType parameterizedWithTypeVars();

    @Override
    default MethodReference stripMetadata() {
        return this;
    }

    @Override
    default boolean hasTypeArguments() {
        return false;
    }

    @Override
    default List<Type> typeArguments() {
        return Collections.emptyList();
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        return this.typeParameters().stream().anyMatch(v -> v.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

}
