package honeyroasted.jype.type.impl;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.NoneType;
import honeyroasted.jype.type.Type;

import java.util.Objects;
import java.util.Set;

public final class NoneTypeImpl extends AbstractType implements NoneType {
    private final String name;

    public NoneTypeImpl(TypeSystem system, String name) {
        super(system);
        this.name = name;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public boolean equals(Type other, Equality kind, Set<Pair<Type, Type>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        if (kind == Equality.EQUIVALENT && Type.baseCaseEquivalence(this, other, seen)) return true;

        if (other instanceof NoneType nt) {
            return Objects.equals(name, nt.name());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<Type> seen) {
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
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        NoneType copy = this.typeSystem().typeFactory().newNoneType(this.name);
        cache.put(this, copy);

        copy.metadata().inheritFrom(this.metadata().copy(cache));
        return (T) copy;
    }
}
