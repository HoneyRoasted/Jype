package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.solver.TypeMetadata;
import honeyroasted.jype.system.solver.TypeWithMetadata;

import java.util.List;
import java.util.Optional;

public interface ParameterizedClassType extends PossiblyUnmodifiable, Type, ClassType {
    List<Type> typeArguments();

    void setTypeArguments(List<Type> typeArguments);

    void setClassReference(ClassReference classReference);

    ParameterizedClassType directSupertype(ClassType supertypeInstance);

    Optional<ClassType> relativeSupertype(ClassReference superType);

    @Override
    default TypeWithMetadata<ParameterizedClassType> withMetadata(TypeMetadata metadata) {
        return new TypeWithMetadata<>(this, metadata);
    }
}
