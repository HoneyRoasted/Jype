package honeyroasted.jype.type.impl;

import honeyroasted.jype.modify.AbstractPossiblyUnmodifiableType;
import honeyroasted.jype.system.TypeSystem;
import honeyroasted.jype.system.cache.TypeCache;
import honeyroasted.jype.type.IntersectionType;
import honeyroasted.jype.type.Type;
import honeyroasted.jype.type.WildType;

import java.util.Iterator;
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
    public String simpleName() {
        return "?" +
                ((!this.upperBound.isEmpty() && !this.upperBound.equals(Set.of(this.typeSystem().constants().object()))) ?
                        " extends " + this.upperBound.stream().map(Type::simpleName).collect(Collectors.joining(" & ")) : "");
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
    public boolean equals(Object o) {
        if (o instanceof IntersectionType it) {
            return it.equals(this);
        }

        return o != null && o instanceof WildType wt && wt.identity() == this.identity;
    }

    @Override
    public int hashCode() {
        return this.identity;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("? extends ");
        Iterator<Type> iterator = this.upperBound.iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next());
            if (iterator.hasNext()) {
                sb.append(" & ");
            }
        }
        return sb.toString();
    }

}
