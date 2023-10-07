package honeyroasted.jype.type.impl;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.modify.AbstractType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.PrimitiveType;

import java.util.Objects;

public final class PrimitiveTypeImpl extends AbstractType implements PrimitiveType {
    private ClassNamespace namespace;
    private ClassNamespace boxNamespace;

    public PrimitiveTypeImpl(TypeSystem typeSystem, ClassNamespace namespace, ClassNamespace boxNamespace) {
        super(typeSystem);
        this.namespace = namespace;
        this.boxNamespace = boxNamespace;
    }

    @Override public ClassNamespace namespace() {
        return this.namespace;
    }

    @Override public ClassNamespace boxNamespace() {
        return this.boxNamespace;
    }

    @Override public String name() {
        return this.namespace().name().value();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof PrimitiveType)) return false;
        PrimitiveType that = (PrimitiveType) o;
        return Objects.equals(namespace, that.namespace());
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace);
    }

    @Override
    public String toString() {
        return this.namespace().name().toString();
    }

}
