package honeyroasted.jype.type;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;

import java.util.ArrayList;
import java.util.List;

public final class MethodReference extends AbstractPossiblyUnmodifiableType {
    private MethodLocation location;
    private Type returnType;
    private List<VarType> typeParameters = new ArrayList<>();

    public MethodReference(TypeSystem typeSystem) {
        super(typeSystem);
    }

    public MethodType asMethodType(List<Type> typeArguments) {
        MethodType methodType = new MethodType(this.typeSystem());
        methodType.setMethodReference(this);
        methodType.setTypeArguments(typeArguments);
        methodType.setUnmodifiable(true);
        return methodType;
    }

    public MethodType asMethodType(Type... typeArguments) {
        return asMethodType(List.of(typeArguments));
    }

    @Override
    protected void makeUnmodifiable() {
        this.typeParameters = List.copyOf(this.typeParameters);
    }

    @Override
    protected void makeModifiable() {
        this.typeParameters = new ArrayList<>(this.typeParameters);
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

    @Override
    public String toString() {
        return this.location.toString();
    }
}
