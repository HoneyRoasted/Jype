package honeyroasted.jype.type;

import honeyroasted.jype.system.visitor.visitors.JVarTypeResolveVisitor;

import java.util.List;

public interface JParameterizedType extends JGenericDeclaration, JType {

    List<JArgumentType> typeArguments();

    void setTypeArguments(List<JArgumentType> typeArguments);

    default JVarTypeResolveVisitor varTypeResolver() {
        return new JVarTypeResolveVisitor(varType -> this.typeParameters().contains(varType),
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
