package honeyroasted.jype.type.impl;

import honeyroasted.jype.modify.AbstractType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.MetaVarType;
import honeyroasted.jype.type.Type;

import java.util.LinkedHashSet;
import java.util.Objects;
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
    public String simpleName() {
        return this.name + "/" + Integer.toString(this.identity, 16);
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        MetaVarType mvt = new MetaVarTypeImpl(this.typeSystem(), this.identity, this.name);
        cache.put(this, mvt);

        this.upperBounds.stream().map(t -> (Type) t.copy(cache)).forEach(mvt.upperBounds()::add);
        this.lowerBounds.stream().map(t -> (Type) t.copy(cache)).forEach(mvt.lowerBounds()::add);
        this.equalities.stream().map(t -> (Type) t.copy(cache)).forEach(mvt.equalities()::add);

        return (T) mvt;
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
