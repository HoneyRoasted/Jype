package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.solver.TypeMetadata;
import honeyroasted.jype.system.solver.TypeWithMetadata;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface MethodReference extends PossiblyUnmodifiable, Type, MethodType {
    ParameterizedMethodType asMethodType(List<Type> typeArguments);

    ParameterizedMethodType asMethodType(Type... typeArguments);

    ParameterizedMethodType parameterizedWithTypeVars();

    @Override
    default boolean hasTypeArguments() {
        return false;
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        return this.typeParameters().stream().anyMatch(v -> v.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

    @Override
    default TypeWithMetadata<MethodReference> withMetadata(TypeMetadata metadata) {
        return new TypeWithMetadata<>(this, metadata);
    }
}
