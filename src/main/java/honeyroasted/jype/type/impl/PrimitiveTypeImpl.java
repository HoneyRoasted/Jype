package honeyroasted.jype.type.impl;

import honeyroasted.jype.location.ClassNamespace;
import honeyroasted.jype.modify.AbstractType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.ClassReference;
import honeyroasted.jype.type.PrimitiveType;
import honeyroasted.jype.type.Type;

import java.util.Objects;
import java.util.Set;

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
    public boolean equals(Type other, Set<Type> seen) {
        if (seen.contains(this)) return true;

        if (other instanceof PrimitiveType pt) {
            return Objects.equals(descriptor, pt.descriptor());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<Type> seen) {
        if (seen.contains(this)) return 0;
        return Objects.hashCode(descriptor);
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        return (T) this;
    }
}
