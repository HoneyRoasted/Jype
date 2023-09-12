package honeyroasted.jype.type;

import honeyroasted.jype.system.TypeSystem;

import java.util.List;
import java.util.Objects;

public class MethodType extends AbstractPossiblyUnmodifiableType {
    private Type returnType;
    private List<Type> typeArguments;
    private List<VarType> typeParameters;

    public MethodType(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    protected void makeUnmodifiable() {
        this.typeArguments = List.copyOf(this.typeArguments);
        this.typeParameters = List.copyOf(this.typeParameters);
    }

    public Type returnType() {
        return this.returnType;
    }

    public void setReturnType(Type returnType) {
        super.checkUnmodifiable();
        this.returnType = returnType;
    }

    public List<Type> typeArguments() {
        return this.typeArguments;
    }

    public void setTypeArguments(List<Type> typeArguments) {
        super.checkUnmodifiable();
        this.typeArguments = typeArguments;
    }

    public List<VarType> typeParameters() {
        return this.typeParameters;
    }

    public void setTypeParameters(List<VarType> typeParameters) {
        super.checkUnmodifiable();
        this.typeParameters = typeParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodType that = (MethodType) o;
        return Objects.equals(returnType, that.returnType) && Objects.equals(typeArguments, that.typeArguments) && Objects.equals(typeParameters, that.typeParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnType, typeArguments, typeParameters);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.returnType).append(" (");

        if (!this.typeParameters().isEmpty() && this.typeArguments().isEmpty()) {
            sb.append("<");
            for (int i = 0; i < this.typeParameters().size(); i++) {
                sb.append(this.typeParameters().get(i));
                if (i < this.typeParameters().size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(">");
        } else if (!this.typeArguments().isEmpty()) {
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
