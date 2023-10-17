package honeyroasted.jype.type.impl;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.modify.AbstractType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

import java.util.Objects;

public final class PrimitiveTypeImpl extends AbstractType implements PrimitiveType {
    private ClassNamespace namespace;
    private ClassReference box;
    private String descriptor;

    public PrimitiveTypeImpl(TypeSystem typeSystem, ClassNamespace namespace, ClassReference box, String descriptor) {
        super(typeSystem);
        this.namespace = namespace;
        this.box = box;
        this.descriptor = descriptor;
    }

    @Override
    public ClassNamespace namespace() {
        return this.namespace;
    }

    @Override
    public ClassReference box() {
        return this.box;
    }

    @Override
    public String descriptor() {
        return this.descriptor;
    }

    @Override
    public String name() {
        return this.namespace().name().value();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof IntersectionType it) {
            return it.equals(this);
        }

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

    @Override
    public String simpleName() {
        return this.toString();
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        return (T) this;
    }
}
