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

public class JWildTypeLowerImpl extends JAbstractPossiblyUnmodifiableType implements JWildType.Lower {
    private Set<JType> lowerBound = new LinkedHashSet<>();
    private int identity;

    public JWildTypeLowerImpl(JTypeSystem typeSystem) {
        super(typeSystem);
        this.identity = System.identityHashCode(this);
    }

    @Override
    public <T extends JType> T copy(JTypeCache<JType, JType> cache) {
        Optional<JType> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        JWildType.Lower copy = this.typeSystem().typeFactory().newLowerWildType();
        cache.put(this, copy);

        copy.metadata().inheritFrom(this.metadata().copy(cache));
        copy.setIdentity(this.identity);
        copy.setLowerBounds(this.lowerBound.stream().map(t -> (JType) t.copy(cache)).collect(Collectors.toCollection(LinkedHashSet::new)));
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    protected void makeModifiable() {
        this.lowerBound = new LinkedHashSet<>(this.lowerBound);
    }

    @Override
    protected void makeUnmodifiable() {
        this.lowerBound = JWildType.linkedCopyOf(this.lowerBound);
    }

    @Override
    public int identity() {
        return this.identity;
    }

    @Override
    public void setIdentity(int identity) {
        this.checkUnmodifiable();
        this.identity = identity;
    }

    @Override
    public Set<JType> upperBounds() {
        return Set.of(this.typeSystem().constants().object());
    }

    @Override
    public void setUpperBounds(Set<JType> upperBounds) {

    }

    @Override
    public Set<JType> lowerBounds() {
        return this.lowerBound;
    }

    public void setLowerBounds(Set<JType> lowerBound) {
        super.checkUnmodifiable();
        this.lowerBound = lowerBound;
    }

    @Override
    public boolean equals(JType other, Equality kind, Set<Pair<JType, JType>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        if (kind == Equality.EQUIVALENT && JType.baseCaseEquivalence(this, other, seen)) return true;
        seen = JType.concat(seen, Pair.identity(this, other));

        if (other instanceof JWildType.Lower wtl) {
            return identity == wtl.identity() && JType.equals(lowerBound, wtl.lowerBounds(), kind, seen);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<JType> seen) {
        if (seen.contains(this)) return 0;
        seen = JType.concat(seen, this);

        return JType.multiHash(identity, JType.hashCode(lowerBound, seen));
    }

}
