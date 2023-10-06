package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.solver.TypeMetadata;
import honeyroasted.jype.system.solver.TypeWithMetadata;

import java.util.List;

public interface ClassReference extends PossiblyUnmodifiable, Type, ClassType {
    ParameterizedClassType parameterized(List<Type> typeArguments);

    ParameterizedClassType parameterized(Type... typeArguments);

    ParameterizedClassType parameterizedWithTypeVars();

    @Override
    default TypeWithMetadata<ClassReference> withMetadata(TypeMetadata metadata) {
        return new TypeWithMetadata<>(this, metadata);
    }
}
