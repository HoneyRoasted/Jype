package honeyroasted.jype.type;

import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ClassType extends AbstractPossiblyUnmodifiableType {
    private ClassReference classReference;
    private List<Type> typeArguments;

    public ClassType(TypeSystem typeSystem) {
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

    public ClassReference classReference() {
        return this.classReference;
    }

    public void setClassReference(ClassReference classReference) {
        super.checkUnmodifiable();
        this.classReference = classReference;
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
        ClassType classType = (ClassType) o;
        return Objects.equals(classReference, classType.classReference) && Objects.equals(typeArguments, classType.typeArguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classReference, typeArguments);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.classReference);
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
        return visitor.visitClass(this, context);
    }
}
