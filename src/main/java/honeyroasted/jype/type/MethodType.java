package honeyroasted.jype.type;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.modify.PossiblyUnmodifiable;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.lang.reflect.Modifier;
import java.util.List;

public interface MethodType extends Type, PossiblyUnmodifiable {

    MethodLocation location();

    void setLocation(MethodLocation location);

    int modifiers();

    void setModifiers(int modifiers);

    ClassReference outerClass();

    void setOuterClass(ClassReference outerClass);

    default boolean hasRelevantOuterType() {
        return !Modifier.isStatic(this.modifiers()) && this.outerType() != null;
    }

    MethodReference methodReference();

    ClassType outerType();

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

    List<ArgumentType> typeArguments();

    @Override
    default <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitMethodType(this, context);
    }
}
