package honeyroasted.jype.type;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.ArrayList;
import java.util.List;

public final class MethodReference extends AbstractPossiblyUnmodifiableType {
    private MethodLocation location;
    private Type returnType;
    private List<Type> parameters = new ArrayList<>();
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
    public <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitMethodRef(this, context);
    }
}
