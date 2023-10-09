package honeyroasted.jype.type.delegate;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.ParameterizedMethodType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.List;
import java.util.function.Function;

public class MethodReferenceDelegate extends AbstractTypeDelegate<MethodReference> implements MethodReference {

    public MethodReferenceDelegate(TypeSystem system, Function<TypeSystem, MethodReference> factory) {
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
    public ParameterizedMethodType asMethodType(List<Type> typeArguments) {
        return new ParameterizedMethodTypeDelegate(this.typeSystem(), ts -> this.delegate().asMethodType(typeArguments));
    }

    @Override
    public ParameterizedMethodType asMethodType(Type... typeArguments) {
        return new ParameterizedMethodTypeDelegate(this.typeSystem(), ts -> this.delegate().asMethodType(typeArguments));
    }

    @Override
    public ParameterizedMethodType parameterizedWithTypeVars() {
        return new ParameterizedMethodTypeDelegate(this.typeSystem(), ts -> this.delegate().parameterizedWithTypeVars());
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
}
