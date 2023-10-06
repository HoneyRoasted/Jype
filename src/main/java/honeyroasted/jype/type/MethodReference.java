package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.solver.TypeMetadata;
import honeyroasted.jype.system.solver.TypeWithMetadata;

import java.util.List;

public interface MethodReference extends PossiblyUnmodifiable, Type, MethodType {
    ParameterizedMethodType asMethodType(List<Type> typeArguments);

    ParameterizedMethodType asMethodType(Type... typeArguments);

    ParameterizedMethodType parameterizedWithTypeVars();

    @Override
    default TypeWithMetadata<MethodReference> withMetadata(TypeMetadata metadata) {
        return new TypeWithMetadata<>(this, metadata);
    }
}
