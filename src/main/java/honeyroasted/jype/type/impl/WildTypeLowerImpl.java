package honeyroasted.jype.type.impl;

import honeyroasted.jype.modify.Pair;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class WildTypeLowerImpl extends AbstractPossiblyUnmodifiableType implements WildType.Lower {
    private Set<Type> lowerBound = new LinkedHashSet<>();
    private int identity;

    public WildTypeLowerImpl(TypeSystem typeSystem) {
        super(typeSystem);
        this.identity = System.identityHashCode(this);
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        Optional<Type> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        WildType.Lower copy = this.typeSystem().typeFactory().newLowerWildType();
        cache.put(this, copy);

        copy.metadata().copyFrom(this.metadata(), cache);
        copy.setIdentity(this.identity);
        copy.setLowerBounds(this.lowerBound.stream().map(t -> (Type) t.copy(cache)).collect(Collectors.toCollection(LinkedHashSet::new)));
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    protected void makeModifiable() {
        this.lowerBound = new LinkedHashSet<>(this.lowerBound);
    }

    @Override
    protected void makeUnmodifiable() {
        this.lowerBound = WildType.linkedCopyOf(this.lowerBound);
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
    public Set<Type> upperBounds() {
        return Set.of(this.typeSystem().constants().object());
    }

    @Override
    public void setUpperBounds(Set<Type> upperBounds) {

    }

    @Override
    public Set<Type> lowerBounds() {
        return this.lowerBound;
    }

    public void setLowerBounds(Set<Type> lowerBound) {
        super.checkUnmodifiable();
        this.lowerBound = lowerBound;
    }

    @Override
    public boolean equals(Type other, Equality kind, Set<Pair<Type, Type>> seen) {
        if (seen.contains(Pair.identity(this, other))) return true;
        if (kind == Equality.EQUIVALENT && Type.baseCaseEquivalence(this, other, seen)) return true;
        seen = Type.concat(seen, Pair.identity(this, other));

        if (other instanceof WildType.Lower wtl) {
            return identity == wtl.identity() && Type.equals(lowerBound, wtl.lowerBounds(), kind, seen);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<Type> seen) {
        if (seen.contains(this)) return 0;
        seen = Type.concat(seen, this);

        return Type.multiHash(identity, Type.hashCode(lowerBound, seen));
    }

}
