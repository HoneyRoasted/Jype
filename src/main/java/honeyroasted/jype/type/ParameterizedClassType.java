package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.solver.TypeMetadata;
import honeyroasted.jype.system.solver.TypeWithMetadata;
import honeyroasted.jype.system.visitor.TypeVisitors;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ParameterizedClassType extends PossiblyUnmodifiable, Type, ClassType {
    List<Type> typeArguments();

    void setTypeArguments(List<Type> typeArguments);

    void setClassReference(ClassReference classReference);

    ParameterizedClassType directSupertype(ClassType supertypeInstance);

    TypeVisitors.Mapping<TypeCache<Type, Type>> varTypeResolver();

    Optional<ClassType> relativeSupertype(ClassReference superType);

    @Override
    default boolean hasTypeArguments() {
        return !this.typeArguments().isEmpty();
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        return this.typeArguments().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

    @Override
    default TypeWithMetadata<ParameterizedClassType> withMetadata(TypeMetadata metadata) {
        return new TypeWithMetadata<>(this, metadata);
    }
}
