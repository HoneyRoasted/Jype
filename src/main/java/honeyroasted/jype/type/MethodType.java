package honeyroasted.jype.type;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.modify.PossiblyUnmodifiable;

import java.util.List;

public sealed interface MethodType extends Type, PossiblyUnmodifiable permits MethodReference, ParameterizedMethodType {

    MethodLocation location();

    void setLocation(MethodLocation location);

    Type returnType();

    void setReturnType(Type returnType);

    List<VarType> typeParameters();

    void setTypeParameters(List<VarType> typeParameters);

    List<Type> parameters();

    void setParameters(List<Type> parameters);

}
