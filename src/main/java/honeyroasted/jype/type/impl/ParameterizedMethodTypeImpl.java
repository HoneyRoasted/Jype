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

public final class ParameterizedMethodTypeImpl extends AbstractPossiblyUnmodifiableType implements ParameterizedMethodType {
    private MethodReference methodReference;
    private List<Type> typeArguments;

    public ParameterizedMethodTypeImpl(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    protected void makeUnmodifiable() {
        this.typeArguments = List.copyOf(this.typeArguments);
    }

    @Override
    protected void makeModifiable() {
        this.typeArguments = new ArrayList<>(this.typeArguments);
    }

    public MethodReference methodReference() {
        return this.methodReference;
    }

    @Override
    public void setMethodReference(MethodReference methodReference) {
        super.checkUnmodifiable();
        this.methodReference = methodReference;
    }

    @Override
    public List<Type> typeArguments() {
        return this.typeArguments;
    }

    @Override
    public void setTypeArguments(List<Type> typeArguments) {
        super.checkUnmodifiable();
        this.typeArguments = typeArguments;
    }

    @Override
    public MethodLocation location() {
        return methodReference.location();
    }

    @Override
    public void setLocation(MethodLocation location) {
        methodReference.setLocation(location);
    }

    @Override
    public Type returnType() {
        return methodReference.returnType();
    }

    @Override
    public void setReturnType(Type returnType) {
        methodReference.setReturnType(returnType);
    }

    @Override
    public List<VarType> typeParameters() {
        return methodReference.typeParameters();
    }

    @Override
    public void setTypeParameters(List<VarType> typeParameters) {
        methodReference.setTypeParameters(typeParameters);
    }

    @Override
    public List<Type> parameters() {
        return methodReference.parameters();
    }

    @Override
    public void setParameters(List<Type> parameters) {
        methodReference.setParameters(parameters);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ParameterizedMethodType)) return false;
        ParameterizedMethodType that = (ParameterizedMethodType) o;
        return Objects.equals(methodReference, that.methodReference()) && Objects.equals(typeArguments, that.typeArguments());
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodReference, typeArguments);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.methodReference);
        if (!this.typeArguments().isEmpty()) {
            sb.append("<");
            for (int i = 0; i < this.typeArguments().size(); i++) {
                sb.append(this.typeArguments().get(i));
                if (i < this.typeArguments().size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(">");
        }
        return sb.toString();
    }
}
