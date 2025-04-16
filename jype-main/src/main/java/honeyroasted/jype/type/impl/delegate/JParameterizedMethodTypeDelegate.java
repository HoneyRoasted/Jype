package honeyroasted.jype.type.impl.delegate;

import honeyroasted.jype.metadata.location.JMethodLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedMethodType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.List;
import java.util.function.Function;

public class JParameterizedMethodTypeDelegate extends JAbstractPossiblyUnmodifiableDelegateType<JParameterizedMethodType> implements JParameterizedMethodType {
    public JParameterizedMethodTypeDelegate(JTypeSystem system, Function<JTypeSystem, JParameterizedMethodType> factory) {
        super(system, factory, JParameterizedMethodTypeDelegate::new);
    }

    @Override
    public JMethodLocation location() {
        return delegate().location();
    }

    @Override
    public void setLocation(JMethodLocation location) {
        delegate().setLocation(location);
    }

    @Override
    public int modifiers() {
        return delegate().modifiers();
    }

    @Override
    public void setModifiers(int modifiers) {
        delegate().setModifiers(modifiers);
    }

    @Override
    public JClassReference outerClass() {
        return delegate().outerClass();
    }

    @Override
    public void setOuterClass(JClassReference outerClass) {
        delegate().setOuterClass(outerClass);
    }

    @Override
    public JMethodReference methodReference() {
        return delegate().methodReference();
    }

    @Override
    public JClassType outerType() {
        return delegate().outerType();
    }

    @Override
    public JType returnType() {
        return delegate().returnType();
    }

    @Override
    public void setReturnType(JType returnType) {
        delegate().setReturnType(returnType);
    }

    @Override
    public List<JType> exceptionTypes() {
        return delegate().exceptionTypes();
    }

    @Override
    public void setExceptionTypes(List<JType> exceptionTypes) {
        delegate().setExceptionTypes(exceptionTypes);
    }

    @Override
    public List<JVarType> typeParameters() {
        return delegate().typeParameters();
    }

    @Override
    public void setTypeParameters(List<JVarType> typeParameters) {
        delegate().setTypeParameters(typeParameters);
    }

    @Override
    public List<JType> parameters() {
        return delegate().parameters();
    }

    @Override
    public void setParameters(List<JType> parameters) {
        delegate().setParameters(parameters);
    }

    @Override
    public List<JArgumentType> typeArguments() {
        return delegate().typeArguments();
    }

    @Override
    public void setTypeArguments(List<JArgumentType> typeArguments) {
        delegate().setTypeArguments(typeArguments);
    }

    @Override
    public void setOuterType(JClassType outerType) {
        delegate().setOuterType(outerType);
    }

    @Override
    public void setMethodReference(JMethodReference methodReference) {
        delegate().setMethodReference(methodReference);
    }
}
