package honeyroasted.jype.type;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.List;

public interface MethodType extends Type, PossiblyUnmodifiable {

    MethodLocation location();

    void setLocation(MethodLocation location);

    int modifiers();

    void setModifiers(int modifiers);

    ClassReference outerClass();

    void setOuterClass(ClassReference outerClass);

    Type returnType();

    void setReturnType(Type returnType);

    default boolean hasTypeParameters() {
        return !this.typeParameters().isEmpty();
    }

    List<Type> exceptionTypes();

    void setExceptionTypes(List<Type> exceptionTypes);

    List<VarType> typeParameters();

    void setTypeParameters(List<VarType> typeParameters);

    List<Type> parameters();

    void setParameters(List<Type> parameters);

    boolean hasTypeArguments();

    List<Type> typeArguments();

    @Override
    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitMethodType(this, context);
    }
}
