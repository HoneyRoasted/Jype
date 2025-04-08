package honeyroasted.jype.type;

import honeyroasted.collect.modify.PossiblyUnmodifiable;
import honeyroasted.jype.metadata.location.JMethodLocation;
import honeyroasted.jype.system.visitor.JTypeVisitor;
import org.glavo.classfile.AccessFlag;

import java.lang.reflect.Modifier;
import java.util.List;

public interface JMethodType extends JType, PossiblyUnmodifiable {

    JMethodLocation location();

    void setLocation(JMethodLocation location);

    int modifiers();

    default JAccess access() {
        return JAccess.fromFlags(modifiers());
    }

    default boolean hasModifier(JAccess flag) {
        return flag.canAccess(this.modifiers());
    }

    default boolean hasModifier(AccessFlag flag) {
        return (flag.mask() & modifiers()) != 0;
    }

    void setModifiers(int modifiers);

    JClassReference outerClass();

    void setOuterClass(JClassReference outerClass);

    default boolean hasRelevantOuterType() {
        return !Modifier.isStatic(this.modifiers()) && this.outerType() != null;
    }

    JMethodReference methodReference();

    JClassType outerType();

    JType returnType();

    void setReturnType(JType returnType);

    default boolean hasTypeParameters() {
        return !this.typeParameters().isEmpty();
    }

    List<JType> exceptionTypes();

    void setExceptionTypes(List<JType> exceptionTypes);

    List<JVarType> typeParameters();

    void setTypeParameters(List<JVarType> typeParameters);

    List<JType> parameters();

    void setParameters(List<JType> parameters);

    boolean hasTypeArguments();

    List<JArgumentType> typeArguments();

    @Override
    default <R, P> R accept(JTypeVisitor<R, P> visitor, P context) {
        return visitor.visitMethodType(this, context);
    }
}
