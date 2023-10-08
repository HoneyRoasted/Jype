package honeyroasted.jype.type;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.List;

public interface MethodType extends Type, PossiblyUnmodifiable {

    MethodLocation location();

    void setLocation(MethodLocation location);

    Type returnType();

    void setReturnType(Type returnType);

    List<VarType> typeParameters();

    void setTypeParameters(List<VarType> typeParameters);

    List<Type> parameters();

    void setParameters(List<Type> parameters);

    boolean hasTypeArguments();

    List<Type> typeArguments();

    @Override
    default MethodType stripMetadata() {
        return this;
    }

    @Override
    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitMethodType(this, context);
    }
}
