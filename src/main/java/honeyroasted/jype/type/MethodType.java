package honeyroasted.jype.type;

import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MethodType extends AbstractPossiblyUnmodifiableType {
    private MethodReference methodReference;
    private List<Type> typeArguments;

    public MethodType(TypeSystem typeSystem) {
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodType that = (MethodType) o;
        return Objects.equals(methodReference, that.methodReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodReference);
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
