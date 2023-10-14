package honeyroasted.jype.type;

import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;

import java.util.List;

public interface ParameterizedType extends Type {

    List<VarType> typeParameters();

    void setTypeParameters(List<VarType> typeParameters);

    List<ArgumentType> typeArguments();

    void setTypeArguments(List<ArgumentType> typeArguments);

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

    default boolean hasTypeArguments() {
        return !this.typeArguments().isEmpty();
    }

}
