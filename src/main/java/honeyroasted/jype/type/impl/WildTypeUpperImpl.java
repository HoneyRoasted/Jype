package honeyroasted.jype.type.impl;

import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class WildTypeUpperImpl extends AbstractPossiblyUnmodifiableType implements WildType.Upper {
    private Set<Type> upperBound = new LinkedHashSet<>();
    private int identity;

    public WildTypeUpperImpl(TypeSystem typeSystem) {
        super(typeSystem);
    }

    @Override
    public <T extends Type> T copy(TypeCache<Type, Type> cache) {
        Optional<Type> cached = cache.get(this);
        if (cached.isPresent()) return (T) cached.get();

        WildType.Upper copy = new WildTypeUpperImpl(this.typeSystem());
        copy.setIdentity(this.identity);
        copy.setUpperBounds(this.upperBound.stream().map(t -> (Type) t.copy(cache)).collect(Collectors.toCollection(LinkedHashSet::new)));
        copy.setUnmodifiable(true);
        return (T) copy;
    }

    @Override
    protected void makeUnmodifiable() {
        this.upperBound = WildType.linkedCopyOf(this.upperBound);
    }

    @Override
    protected void makeModifiable() {
        this.upperBound = new LinkedHashSet<>(this.upperBound);
    }

    @Override
    public void setLowerBounds(Set<Type> lowerBounds) {
        throw new UnsupportedOperationException("Cannot set lower bounds on WildType.Upper implementation");
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
    public Set<Type> upperBounds() {
        return this.upperBound;
    }

    @Override
    public Set<Type> lowerBounds() {
        return Set.of(this.typeSystem().constants().nullType());
    }

    @Override
    public void setUpperBounds(Set<Type> upperBound) {
        this.checkUnmodifiable();
        this.upperBound = upperBound;
    }

    @Override
    public boolean equals(Type other, Set<Type> seen) {
        if (seen.contains(this)) return true;
        seen = Type.concat(seen, this);

        if (other instanceof WildType.Lower wtl) {
            return identity == wtl.identity() && Type.equals(upperBound, wtl.lowerBounds(), seen);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(Set<Type> seen) {
        if (seen.contains(this)) return 0;
        seen = Type.concat(seen, this);

        return Type.multiHash(identity, Type.hashCode(upperBound, seen));
    }

}
