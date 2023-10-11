package honeyroasted.jype.type.impl;

import honeyroasted.jype.modify.AbstractType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

import java.util.Objects;

public class MetaVarTypeImpl extends AbstractType implements MetaVarType {
    private int identity;
    private String name;

    public MetaVarTypeImpl(TypeSystem typeSystem, int identity, String name) {
        super(typeSystem);
        this.identity = identity;
        this.name = name;
    }

    public MetaVarTypeImpl(TypeSystem typeSystem, String name) {
        super(typeSystem);
        this.name = name;
        this.identity = System.identityHashCode(this);
    }

    @Override
    public int identity() {
        return this.identity;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        return (T) this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetaVarTypeImpl that = (MetaVarTypeImpl) o;
        return identity == that.identity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identity);
    }

    @Override
    public String toString() {
        return "%" + this.name + "/" + Integer.toString(this.identity, 16);
    }
}
