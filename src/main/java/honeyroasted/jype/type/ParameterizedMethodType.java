package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface ParameterizedMethodType extends PossiblyUnmodifiable, MethodType, ParameterizedType {
    MethodReference methodReference();

    ClassType outerType();

    void setOuterType(ClassType outerType);

    void setMethodReference(MethodReference methodReference);

    @Override
    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        return this.typeArguments().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

    default boolean hasTypeArguments() {
        return !this.typeArguments().isEmpty();
    }

}
