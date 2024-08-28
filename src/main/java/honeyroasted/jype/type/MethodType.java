package honeyroasted.jype.type;

import honeyroasted.collect.modify.PossiblyUnmodifiable;
import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Modifier;
import java.util.List;

public interface MethodType extends Type, PossiblyUnmodifiable {

    MethodLocation location();

    void setLocation(MethodLocation location);

    int modifiers();

    default Access access() {
        return Access.fromFlags(modifiers());
    }

    default boolean hasModifier(Access flag) {
        return flag.canAccess(this.modifiers());
    }

    default boolean hasModifier(AccessFlag flag) {
        return (flag.mask() & modifiers()) != 0;
    }

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
