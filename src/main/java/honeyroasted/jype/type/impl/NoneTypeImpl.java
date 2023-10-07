package honeyroasted.jype.type.impl;

import honeyroasted.jype.modify.AbstractType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.type.NoneType;

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

}
