package honeyroasted.jype.type.impl;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

import java.util.LinkedHashSet;
import java.util.Set;

public class MetaVarTypeImpl extends AbstractType implements MetaVarType {
    private int identity;
    private String name;

    private Set<Type> lowerBounds = new LinkedHashSet<>();
    private Set<Type> upperBounds = new LinkedHashSet<>();
    private Set<Type> equalities = new LinkedHashSet<>();

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
    public Set<Type> upperBounds() {
        return this.lowerBounds;
    }

    @Override
    public Set<Type> lowerBounds() {
        return this.upperBounds;
    }

    @Override
    public Set<Type> equalities() {
        return this.equalities;
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        MetaVarType copy = this.typeSystem().typeFactory().newMetaVarType(this.identity, this.name);
        cache.put(this, copy);

        copy.metadata().inheritFrom(this.metadata().copy(cache));
        this.upperBounds.stream().map(t -> (Type) t.copy(cache)).forEach(copy.upperBounds()::add);
        this.lowerBounds.stream().map(t -> (Type) t.copy(cache)).forEach(copy.lowerBounds()::add);
        this.equalities.stream().map(t -> (Type) t.copy(cache)).forEach(copy.equalities()::add);

        return (T) copy;
    }

    @Override
    public boolean equals(Type other, Equality kind, Set<Pair<Type, Type>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        if (kind == Equality.EQUIVALENT && Type.baseCaseEquivalence(this, other, seen)) return true;

        if (other instanceof MetaVarType mvt) {
            return this.identity == mvt.identity();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<Type> seen) {
        if (seen.contains(this)) return 0;

        return this.identity;
    }

}
