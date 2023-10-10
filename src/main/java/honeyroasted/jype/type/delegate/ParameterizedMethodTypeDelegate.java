package honeyroasted.jype.type.delegate;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.visitors.VarTypeResolveVisitor;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.ClassType;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.ParameterizedMethodType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.List;
import java.util.function.Function;

public class ParameterizedMethodTypeDelegate extends AbstractTypeDelegate<ParameterizedMethodType> implements ParameterizedMethodType {

    public ParameterizedMethodTypeDelegate(TypeSystem system, Function<TypeSystem, ParameterizedMethodType> factory) {
        super(system, factory);
    }

    @Override
    public boolean isUnmodifiable() {
        return this.delegate().isUnmodifiable();
    }

    @Override
    public void setUnmodifiable(boolean unmodifiable) {
        this.delegate().setUnmodifiable(unmodifiable);
    }

    @Override
    public MethodLocation location() {
        return this.delegate().location();
    }

    @Override
    public void setLocation(MethodLocation location) {
        this.delegate().setLocation(location);
    }

    @Override
    public int modifiers() {
        return this.delegate().modifiers();
    }

    @Override
    public void setModifiers(int modifiers) {
        this.delegate().setModifiers(modifiers);
    }

    @Override
    public ClassReference outerClass() {
        return this.delegate().outerClass();
    }

    @Override
    public void setOuterClass(ClassReference outerClass) {
        this.delegate().setOuterClass(outerClass);
    }

    @Override
    public Type returnType() {
        return this.delegate().returnType();
    }

    @Override
    public void setReturnType(Type returnType) {
        this.delegate().setReturnType(returnType);
    }

    @Override
    public List<Type> exceptionTypes() {
        return this.delegate().exceptionTypes();
    }

    @Override
    public void setExceptionTypes(List<Type> exceptionTypes) {
        this.delegate().setExceptionTypes(exceptionTypes);
    }


    @Override
    public List<VarType> typeParameters() {
        return this.delegate().typeParameters();
    }

    @Override
    public void setTypeParameters(List<VarType> typeParameters) {
        this.delegate().setTypeParameters(typeParameters);
    }

    @Override
    public List<Type> parameters() {
        return this.delegate().parameters();
    }

    @Override
    public void setParameters(List<Type> parameters) {
        this.delegate().setParameters(parameters);
    }

    @Override
    public MethodReference methodReference() {
        return this.delegate().methodReference();
    }

    @Override
    public ClassType outerType() {
        return this.delegate().outerType();
    }

    @Override
    public void setOuterType(ClassType outerType) {
        this.delegate().setOuterType(outerType);
    }

    @Override
    public void setMethodReference(MethodReference methodReference) {
        this.delegate().setMethodReference(methodReference);
    }

    @Override
    public List<Type> typeArguments() {
        return this.delegate().typeArguments();
    }

    @Override
    public void setTypeArguments(List<Type> typeArguments) {
        this.delegate().setTypeArguments(typeArguments);
    }

    @Override
    public VarTypeResolveVisitor varTypeResolver() {
        return this.delegate().varTypeResolver();
    }
}
