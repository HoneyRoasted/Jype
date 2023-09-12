package honeyroasted.jype.type;

import honeyroasted.jype.model.name.ClassLocation;
import honeyroasted.jype.model.name.ClassName;
import honeyroasted.jype.model.name.ClassNamespace;
import honeyroasted.jype.system.TypeSystem;

import java.util.List;
import java.util.Objects;

public class PrimitiveType extends AbstractType {
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
}
