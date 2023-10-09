package honeyroasted.jype.type;

import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface ParameterizedMethodType extends PossiblyUnmodifiable, MethodType {
    MethodReference methodReference();

    void setMethodReference(MethodReference methodReference);

    void setTypeArguments(List<Type> typeArguments);

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

    @Override
    default boolean hasTypeArguments() {
        return !this.typeArguments().isEmpty();
    }

    @Override
    default boolean hasCyclicTypeVariables(Set<VarType> seen) {
        return this.typeArguments().stream().anyMatch(t -> t.hasCyclicTypeVariables(new HashSet<>(seen)));
    }

}
