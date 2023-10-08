package honeyroasted.jype.type.impl;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.MethodReference;
import honeyroasted.jype.type.ParameterizedMethodType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.VarType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MethodReferenceImpl extends AbstractPossiblyUnmodifiableType implements MethodReference {
    private MethodLocation location;
    private int modifiers;
    private Type returnType;
    private List<Type> parameters = new ArrayList<>();
    private List<VarType> typeParameters = new ArrayList<>();

    public MethodReferenceImpl(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public ParameterizedMethodType asMethodType(List<Type> typeArguments) {
        ParameterizedMethodType parameterizedMethodType = new ParameterizedMethodTypeImpl(this.typeSystem());
        parameterizedMethodType.setMethodReference(this);
        parameterizedMethodType.setTypeArguments(typeArguments);
        parameterizedMethodType.setUnmodifiable(true);
        return parameterizedMethodType;
    }

    @Override
    public ParameterizedMethodType asMethodType(Type... typeArguments) {
        return asMethodType(List.of(typeArguments));
    }

    @Override
    public ParameterizedMethodType parameterizedWithTypeVars() {
        return this.asMethodType((List<Type>) (List) this.typeParameters);
    }

    @Override
    protected void makeUnmodifiable() {
        this.typeParameters = List.copyOf(this.typeParameters);
        this.parameters = List.copyOf(this.parameters);
    }

    @Override
    protected void makeModifiable() {
        this.typeParameters = new ArrayList<>(this.typeParameters);
        this.parameters = new ArrayList<>(this.parameters);
    }

    public MethodLocation location() {
        return this.location;
    }

    public void setLocation(MethodLocation location) {
        super.checkUnmodifiable();
        this.location = location;
    }

    @Override
    public int modifiers() {
        return this.modifiers;
    }

    @Override
    public void setModifiers(int modifiers) {
        this.checkUnmodifiable();
        this.modifiers = modifiers;
    }

    public Type returnType() {
        return this.returnType;
    }

    public void setReturnType(Type returnType) {
        super.checkUnmodifiable();
        this.returnType = returnType;
    }

    public List<VarType> typeParameters() {
        return this.typeParameters;
    }

    public void setTypeParameters(List<VarType> typeParameters) {
        super.checkUnmodifiable();
        this.typeParameters = typeParameters;
    }

    public List<Type> parameters() {
        return this.parameters;
    }

    public void setParameters(List<Type> parameters) {
        super.checkUnmodifiable();
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return this.location.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof MethodReference)) return false;
        MethodReference that = (MethodReference) o;
        return Objects.equals(location, that.location()) && Objects.equals(returnType, that.returnType()) && Objects.equals(parameters, that.parameters()) && Objects.equals(typeParameters, that.typeParameters());
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, returnType, parameters, typeParameters);
    }

}
