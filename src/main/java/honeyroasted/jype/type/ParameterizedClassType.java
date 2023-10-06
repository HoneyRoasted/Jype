package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.system.solver.TypeMetadata;
import honeyroasted.jype.system.solver.TypeWithMetadata;
import honeyroasted.jype.system.visitor.TypeVisitors;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ParameterizedClassType extends PossiblyUnmodifiable, Type, ClassType {
    List<Type> typeArguments();

    void setTypeArguments(List<Type> typeArguments);

    void setClassReference(ClassReference classReference);

    ParameterizedClassType directSupertype(ClassType supertypeInstance);

    default TypeVisitors.Mapping<TypeCache<Type, Type>> varTypeResolver() {
        return new VarTypeResolveVisitor(varType -> this.typeParameters().contains(varType),
                varType -> {
                    for (int i = 0; i < this.typeArguments().size() && i < this.typeParameters().size(); i++) {
                        if (varType.equals(this.typeParameters().get(i))) {
                            return this.typeArguments().get(i);
                        }
                    }
                    return varType;
                });
    }

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
