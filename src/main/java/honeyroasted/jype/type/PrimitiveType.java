package honeyroasted.jype.type;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.modify.AbstractType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.solver.TypeMetadata;
import honeyroasted.jype.system.solver.TypeWithMetadata;
import honeyroasted.jype.system.visitor.TypeVisitor;

import java.util.Objects;

public final class PrimitiveType extends AbstractType {
    private ClassNamespace namespace;
    private ClassNamespace boxNamespace;

    public PrimitiveType(TypeSystem typeSystem, ClassNamespace namespace, ClassNamespace boxNamespace) {
        super(typeSystem);
        this.namespace = namespace;
        this.boxNamespace = boxNamespace;
    }

    public ClassNamespace namespace() {
        return this.namespace;
    }

    public ClassNamespace boxNamespace() {
        return this.boxNamespace;
    }

    @Override
    public TypeWithMetadata<PrimitiveType> withMetadata(TypeMetadata metadata) {
        return super.withMetadata(metadata);
    }

    public String name() {
        return this.namespace().name().value();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrimitiveType that = (PrimitiveType) o;
        return Objects.equals(namespace, that.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace);
    }

    @Override
    public String toString() {
        return this.namespace().name().toString();
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, P context) {
        return visitor.visitPrimitiveType(this, context);
    }
}
