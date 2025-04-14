package honeyroasted.jype.type.impl;

import honeyroasted.collect.multi.Pair;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.cache.JTypeCache;
import honeyroasted.jype.type.JType;
import honeyroasted.jype.type.JWildType;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JWildTypeUpperImpl extends JAbstractPossiblyUnmodifiableType implements JWildType.Upper {
    private Set<JType> upperBound = new LinkedHashSet<>();
    private int identity;

    public JWildTypeUpperImpl(JTypeSystem typeSystem) {
        super(typeSystem);
        this.identity = System.identityHashCode(this);
    }

    @Override
    public <T extends JType> T copy(JTypeCache<JType, JType> cache) {
        Optional<JType> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        JWildType.Upper copy = this.typeSystem().typeFactory().newUpperWildType();
        cache.put(this, copy);

        copy.metadata().inheritFrom(this.metadata().copy(cache));
        copy.setIdentity(this.identity);
        copy.setUpperBounds(this.upperBound.stream().map(t -> (JType) t.copy(cache)).collect(Collectors.toCollection(LinkedHashSet::new)));
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    protected void makeUnmodifiable() {
        this.upperBound = JWildType.linkedCopyOf(this.upperBound);
    }

    @Override
    protected void makeModifiable() {
        this.upperBound = new LinkedHashSet<>(this.upperBound);
    }

    @Override
    public void setLowerBounds(Set<JType> lowerBounds) {
        throw new UnsupportedOperationException("Cannot set lower interfaceBounds on JWildType.Upper implementation");
    }

    @Override
    public int identity() {
        return this.identity;
    }

    @Override
    public void setIdentity(int identity) {
        this.identity = identity;
    }

    @Override
    public Set<JType> upperBounds() {
        return this.upperBound;
    }

    @Override
    public Set<JType> lowerBounds() {
        return Set.of(this.typeSystem().constants().nullType());
    }

    @Override
    public void setUpperBounds(Set<JType> upperBound) {
        this.checkUnmodifiable();
        this.upperBound = upperBound;
    }

    @Override
    public boolean equals(JType other, Equality kind, Set<Pair<JType, JType>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        if (kind == Equality.EQUIVALENT && JType.baseCaseEquivalence(this, other, seen)) return true;
        seen = JType.concat(seen, Pair.identity(this, other));

        if (other instanceof JWildType.Upper wtu) {
            return identity == wtu.identity() && JType.equals(upperBound, wtu.upperBounds(), kind, seen);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<JType> seen) {
        if (seen.contains(this)) return 0;
        seen = JType.concat(seen, this);

        return JType.multiHash(identity, JType.hashCode(upperBound, seen));
    }

}
