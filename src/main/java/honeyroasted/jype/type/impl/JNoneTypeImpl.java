package honeyroasted.jype.type.impl;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.type.JNoneType;
import honeyroasted.jype.type.JType;

import java.util.Objects;
import java.util.Set;

public final class JNoneTypeImpl extends JAbstractType implements JNoneType {
    private final String name;

    public JNoneTypeImpl(JTypeSystem system, String name) {
        super(system);
        this.name = name;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public boolean equals(JType other, Equality kind, Set<Pair<JType, JType>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        if (kind == Equality.EQUIVALENT && JType.baseCaseEquivalence(this, other, seen)) return true;

        if (other instanceof JNoneType nt) {
            return Objects.equals(name, nt.name());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<JType> seen) {
        if (seen.contains(this)) return 0;
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return "@" + this.name;
    }

    @Override
    public String simpleName() {
        return this.toString();
    }

    @Override
    public <T extends JType> T copy(JTypeCache<JType, JType> cache) {
        JNoneType copy = this.typeSystem().typeFactory().newNoneType(this.name);
        cache.put(this, copy);

        copy.metadata().inheritFrom(this.metadata().copy(cache));
        return (T) copy;
    }
}
