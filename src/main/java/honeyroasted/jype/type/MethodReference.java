package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface MethodReference extends PossiblyUnmodifiable, MethodType {
    ParameterizedMethodType asMethodType(List<ArgumentType> typeArguments);

    ParameterizedMethodType asMethodType(ArgumentType... typeArguments);

    ParameterizedMethodType parameterizedWithTypeVars();

    @Override
    default boolean hasTypeArguments() {
        return false;
    }

    @Override
    default List<ArgumentType> typeArguments() {
        return Collections.emptyList();
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        return this.typeParameters().stream().anyMatch(v -> v.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

}
