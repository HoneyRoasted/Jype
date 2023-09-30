package honeyroasted.jype.type;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MethodReference extends AbstractPossiblyUnmodifiableType implements MethodType {
    private MethodLocation location;
    private Type returnType;
    private List<Type> parameters = new ArrayList<>();
    private List<VarType> typeParameters = new ArrayList<>();

    public MethodReference(TypeSystem typeSystem) {
        super(typeSystem);
    }

    public ParameterizedMethodType asMethodType(List<Type> typeArguments) {
        ParameterizedMethodType parameterizedMethodType = new ParameterizedMethodType(this.typeSystem());
        parameterizedMethodType.setMethodReference(this);
        parameterizedMethodType.setTypeArguments(typeArguments);
        parameterizedMethodType.setUnmodifiable(true);
        return parameterizedMethodType;
    }

    public ParameterizedMethodType asMethodType(Type... typeArguments) {
        return asMethodType(List.of(typeArguments));
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
        if (o == null || getClass() != o.getClass()) return false;
        MethodReference that = (MethodReference) o;
        return Objects.equals(location, that.location) && Objects.equals(returnType, that.returnType) && Objects.equals(parameters, that.parameters) && Objects.equals(typeParameters, that.typeParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, returnType, parameters, typeParameters);
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitMethodType(this, context);
    }
}
