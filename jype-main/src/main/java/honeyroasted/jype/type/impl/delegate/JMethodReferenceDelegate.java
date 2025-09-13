package honeyroasted.jype.type.impl.delegate;

import honeyroasted.jype.metadata.location.JMethodLocation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedMethodType;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JVarType;

import java.util.List;
import java.util.function.Function;

public class JMethodReferenceDelegate extends JAbstractPossiblyUnmodifiableDelegateType<JMethodReference> implements JMethodReference {

    public JMethodReferenceDelegate(JTypeSystem system, Function<JTypeSystem, JMethodReference> factory) {
        super(system, factory, JMethodReferenceDelegate::new);
    }

    @Override
    public JParameterizedMethodType parameterized(List<JArgumentType> typeArguments) {
        return delegate().parameterized(typeArguments);
    }

    @Override
    public JParameterizedMethodType parameterized(JArgumentType... typeArguments) {
        return delegate().parameterized(typeArguments);
    }

    @Override
    public JParameterizedMethodType parameterizedWithTypeVars() {
        return delegate().parameterizedWithTypeVars();
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
}
