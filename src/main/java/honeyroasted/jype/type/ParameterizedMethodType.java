package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.solver.TypeMetadata;
import honeyroasted.jype.system.solver.TypeWithMetadata;

import java.util.List;

public interface ParameterizedMethodType extends PossiblyUnmodifiable, Type, MethodType {
    MethodReference methodReference();

    void setMethodReference(MethodReference methodReference);

    List<Type> typeArguments();

    void setTypeArguments(List<Type> typeArguments);

    @Override
    default TypeWithMetadata<ParameterizedMethodType> withMetadata(TypeMetadata metadata) {
        return new TypeWithMetadata<>(this, metadata);
    }
}
