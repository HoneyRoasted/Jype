package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ParameterizedClassType extends PossiblyUnmodifiable, Type, ClassType {

    void setTypeArguments(List<Type> typeArguments);

    void setClassReference(ClassReference classReference);

    ClassType outerType();

    void setOuterType(ClassType outerType);

    ParameterizedClassType directSupertype(ClassType supertypeInstance);

    @Override
    default ParameterizedClassType stripMetadata() {
        return this;
    }

    default VarTypeResolveVisitor varTypeResolver() {
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

}
