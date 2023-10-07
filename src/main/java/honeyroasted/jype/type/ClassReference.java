package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface ClassReference extends PossiblyUnmodifiable, Type, ClassType {
    ParameterizedClassType parameterized(List<Type> typeArguments);

    ParameterizedClassType parameterized(Type... typeArguments);

    ParameterizedClassType parameterizedWithTypeVars();

    @Override
    default ClassReference stripMetadata() {
        return this;
    }

    @Override
    default boolean hasTypeArguments() {
        return false;
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        return this.typeParameters().stream().anyMatch(v -> v.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

}
