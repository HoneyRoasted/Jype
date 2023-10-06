package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.solver.TypeMetadata;
import honeyroasted.jype.system.solver.TypeWithMetadata;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface ParameterizedMethodType extends PossiblyUnmodifiable, Type, MethodType {
    MethodReference methodReference();

    void setMethodReference(MethodReference methodReference);

    List<Type> typeArguments();

    void setTypeArguments(List<Type> typeArguments);

    @Override
    default boolean hasTypeArguments() {
        return !this.typeArguments().isEmpty();
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        return this.typeArguments().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

    @Override
    default TypeWithMetadata<ParameterizedMethodType> withMetadata(TypeMetadata metadata) {
        return new TypeWithMetadata<>(this, metadata);
    }
}
