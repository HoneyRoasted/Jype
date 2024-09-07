package honeyroasted.jype.type.impl;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.type.JMetaVarType;
import honeyroasted.jype.type.JType;

import java.util.LinkedHashSet;
import java.util.Set;

public class JMetaVarTypeImpl extends JAbstractType implements JMetaVarType {
    private int identity;
    private String name;

    private Set<JType> lowerBounds = new LinkedHashSet<>();
    private Set<JType> upperBounds = new LinkedHashSet<>();
    private Set<JType> equalities = new LinkedHashSet<>();

    public JMetaVarTypeImpl(JTypeSystem typeSystem, int identity, String name) {
        super(typeSystem);
        this.identity = identity;
        this.name = name;
    }

    public JMetaVarTypeImpl(JTypeSystem typeSystem, String name) {
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
    public Set<JType> upperBounds() {
        return this.lowerBounds;
    }

    @Override
    public Set<JType> lowerBounds() {
        return this.upperBounds;
    }

    @Override
    public Set<JType> equalities() {
        return this.equalities;
    }

    @Override
    public <T extends JType> T copy(JTypeCache<JType, JType> cache) {
        JMetaVarType copy = this.typeSystem().typeFactory().newMetaVarType(this.identity, this.name);
        cache.put(this, copy);

        copy.metadata().inheritFrom(this.metadata().copy(cache));
        this.upperBounds.stream().map(t -> (JType) t.copy(cache)).forEach(copy.upperBounds()::add);
        this.lowerBounds.stream().map(t -> (JType) t.copy(cache)).forEach(copy.lowerBounds()::add);
        this.equalities.stream().map(t -> (JType) t.copy(cache)).forEach(copy.equalities()::add);

        return (T) copy;
    }

    @Override
    public boolean equals(JType other, Equality kind, Set<Pair<JType, JType>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        if (kind == Equality.EQUIVALENT && JType.baseCaseEquivalence(this, other, seen)) return true;

        if (other instanceof JMetaVarType mvt) {
            return this.identity == mvt.identity();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<JType> seen) {
        if (seen.contains(this)) return 0;

        return this.identity;
    }

}
