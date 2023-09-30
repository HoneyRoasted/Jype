package honeyroasted.jype.type;

import honeyroasted.jype.location.MethodLocation;
import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ParameterizedMethodType extends AbstractPossiblyUnmodifiableType implements MethodType {
    private MethodReference methodReference;
    private List<Type> typeArguments;

    public ParameterizedMethodType(TypeSystem typeSystem) {
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

    public void setMethodReference(MethodReference methodReference) {
        super.checkUnmodifiable();
        this.methodReference = methodReference;
    }

    public List<Type> typeArguments() {
        return this.typeArguments;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterizedMethodType that = (ParameterizedMethodType) o;
        return Objects.equals(methodReference, that.methodReference) && Objects.equals(typeArguments, that.typeArguments);
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

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitMethodType(this, context);
    }
}
