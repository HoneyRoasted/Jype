package honeyroasted.jype.type.impl;

import honeyroasted.jype.modify.AbstractType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.Type;

import java.util.Objects;

public final class NoneTypeImpl extends AbstractType implements NoneType {
    private final String name;

    public NoneTypeImpl(TypeSystem system, String name) {
        super(system);
        this.name = name;
    }

    @Override public String name() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !( o instanceof NoneType)) return false;
        NoneType noneType = (NoneType) o;
        return Objects.equals(name, noneType.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "@" + this.name;
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        return (T) this;
    }
}
