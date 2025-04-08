package honeyroasted.jype.type.impl.delegate;

import honeyroasted.jype.metadata.location.JClassNamespace;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.type.JArgumentType;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JClassType;
import honeyroasted.jype.type.JMethodReference;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JVarType;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class JParameterizedClassTypeDelegate extends JAbstractPossiblyUnmodifiableDelegateType<JParameterizedClassType> implements JParameterizedClassType {

    public JParameterizedClassTypeDelegate(JTypeSystem system, Function<JTypeSystem, JParameterizedClassType> factory) {
        super(system, factory);
    }

    @Override
    public void setClassReference(JClassReference classReference) {
        delegate().setClassReference(classReference);
    }

    @Override
    public JClassNamespace namespace() {
        return delegate().namespace();
    }

    @Override
    public void setNamespace(JClassNamespace namespace) {
        delegate().setNamespace(namespace);
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
    public JMethodReference outerMethod() {
        return delegate().outerMethod();
    }

    @Override
    public void setOuterMethod(JMethodReference outerMethod) {
        delegate().setOuterMethod(outerMethod);
    }

    @Override
    public List<JMethodReference> declaredMethods() {
        return delegate().declaredMethods();
    }

    @Override
    public void setDeclaredMethods(List<JMethodReference> methods) {
        delegate().setDeclaredMethods(methods);
    }

    @Override
    public JParameterizedClassType directSupertype(JClassType supertypeInstance) {
        return delegate().directSupertype(supertypeInstance);
    }

    @Override
    public Optional<JClassType> relativeSupertype(JClassType superType) {
        return delegate().relativeSupertype(superType);
    }

    @Override
    public JClassType outerType() {
        return delegate().outerType();
    }

    @Override
    public JClassType superClass() {
        return delegate().superClass();
    }

    @Override
    public void setSuperClass(JClassType superClass) {
        delegate().setSuperClass(superClass);
    }

    @Override
    public List<JClassType> interfaces() {
        return delegate().interfaces();
    }

    @Override
    public void setInterfaces(List<JClassType> interfaces) {
        delegate().setInterfaces(interfaces);
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
    public boolean hasSupertype(JClassReference supertype) {
        return delegate().hasSupertype(supertype);
    }

    @Override
    public JClassReference classReference() {
        return delegate().classReference();
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
}
