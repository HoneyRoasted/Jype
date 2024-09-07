package honeyroasted.jype.type.impl;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.location.JClassNamespace;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.type.JClassReference;
import honeyroasted.jype.type.JPrimitiveType;
import honeyroasted.jype.type.JType;

import java.util.Objects;
import java.util.Set;

public final class JPrimitiveTypeImpl extends JAbstractType implements JPrimitiveType {
    private JClassNamespace namespace;
    private JClassReference box;
    private String descriptor;

    public JPrimitiveTypeImpl(JTypeSystem typeSystem, JClassNamespace namespace, JClassReference box, String descriptor) {
        super(typeSystem);
        this.namespace = namespace;
        this.box = box;
        this.descriptor = descriptor;
    }

    @Override
    public JClassNamespace namespace() {
        return this.namespace;
    }

    @Override
    public JClassReference box() {
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
    public boolean equals(JType other, Equality kind, Set<Pair<JType, JType>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        if (kind == Equality.EQUIVALENT && JType.baseCaseEquivalence(this, other, seen)) return true;

        if (other instanceof JPrimitiveType pt) {
            return Objects.equals(descriptor, pt.descriptor());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<JType> seen) {
        if (seen.contains(this)) return 0;
        return Objects.hashCode(descriptor);
    }

    @Override
    public <T extends JType> T copy(JTypeCache<JType, JType> cache) {
        JPrimitiveType copy = this.typeSystem().typeFactory().newPrimitiveType(this.namespace, this.box, this.descriptor);
        cache.put(this, copy);

        copy.metadata().inheritFrom(this.metadata().copy(cache));
        return (T) copy;
    }
}
